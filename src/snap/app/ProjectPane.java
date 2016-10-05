package snap.app;
import java.io.File;
import java.util.List;
import snap.debug.RunApp;
import snap.javatext.JavaPage;
import snap.project.*;
import snap.util.*;
import snap.view.*;
import snap.viewx.*;
import snap.web.*;

/**
 * A class to manage UI aspects of a Project.
 */
public class ProjectPane extends ViewOwner {

    // The AppPane
    AppPane             _appPane;
    
    // The SitePane
    SitePane            _sitePane;
    
    // The WebSite
    WebSite             _site;

    // The project
    Project             _proj;
    
    // The project set
    ProjectSet          _projSet;
    
    // Whether to auto build project when files change
    boolean             _autoBuild = true;

    // Whether to auto build project feature is enabled
    boolean             _autoBuildEnabled = true;

    // The runner to build files
    BuildFilesRunner    _buildFilesRunner;
    
    // The selected JarPath
    String              _jarPath;
    
    // The selected ProjectPath
    String              _projPath;
    
    // Runnable for build later
    Runnable            _buildLaterRun;
    
/**
 * Creates a new ProjectPane for given project.
 */
public ProjectPane(SitePane aSitePane)
{
    _sitePane = aSitePane; _site = aSitePane.getSite(); _site.setProp(ProjectPane.class.getName(), this);
    _proj = Project.get(_site, true); _projSet = _proj.getProjectSet();
}

/**
 * Returns the AppPane.
 */
public AppPane getAppPane()  { return _appPane; }

/**
 * Sets the AppPane.
 */
protected void setAppPane(AppPane anAP)
{
    _appPane = anAP;
    
    // Add listener to update ProcPane and JavaPage.TextView(s) when Breakpoint added/removed
    _proj.getBreakpoints().addPropChangeListener(pc -> {
        System.out.println("Breakpoints.change: " + pc);
        if(pc.getPropertyName()!=Breakpoints.ITEMS_PROP) return;
        Breakpoint oval = (Breakpoint)pc.getOldValue(), nval = (Breakpoint)pc.getNewValue();
        if(nval!=null) notifyBreakpointAdded(nval);
        else notifyBreakpointRemoved(oval);
    });
    
    // Add listener to update SupportTray and JavaPage.TextView(s) when BuildIssue added/removed
    _proj.getBuildIssues().addPropChangeListener(pc -> {
        if(pc.getPropertyName()!=Breakpoints.ITEMS_PROP) return;
        BuildIssue oval = (BuildIssue)pc.getOldValue(), nval = (BuildIssue)pc.getNewValue();
        if(nval!=null) notifyBuildIssueAdded(nval);
        else notifyBuildIssueRemoved(oval);
    });
}

/**
 * Returns the SitePane.
 */
public SitePane getSitePane()  { return _sitePane; }

/**
 * Returns the project.
 */
public Project getProject()  { return _proj; }

/**
 * Returns whether to automatically build files when changes are detected.
 */
public boolean isAutoBuild()  { return _autoBuild; }

/**
 * Sets whether to automatically build files when changes are detected.
 */
public void setAutoBuild(boolean aValue)  { _autoBuild = aValue; }

/**
 * Returns whether to project AutoBuild has been disabled (possibly for batch processing).
 */
public boolean isAutoBuildEnabled()  { return isAutoBuild() && _autoBuildEnabled; }

/**
 * Sets whether to project AutoBuild has been disabled (possibly for batch processing).
 */
public boolean setAutoBuildEnabled(boolean aFlag)  { boolean o =_autoBuildEnabled; _autoBuildEnabled =aFlag; return o; }

/**
 * Activate project.
 */
public void openSite()
{
    // Kick off site build
    if(_sitePane.isAutoBuildEnabled())
        buildProjectLater(true);
}

/**
 * Delete a project.
 */
public void deleteProject(View aView)
{
    _sitePane.setAutoBuild(false);
    try { _proj.deleteProject(new TaskMonitorPanel(aView, "Delete Project")); }
    catch(Exception e) { DialogBox.showExceptionDialog(aView, "Delete Project Failed", e); }
}

/**
 * Adds a project with given name.
 */
public void addProject(String aName, String aURLString)
{
    // Get View
    View view = isUISet() && getUI().isShowing()? getUI() : getAppPane().getUI();
    
    // If already set, just return
    if(_proj.getProjectSet().getProject(aName)!=null) {
        DialogBox.showWarningDialog(view, "Error Adding Project", "Project already present"); return; }

    // Get site - if not present, create and clone
    WebSite site = WelcomePanel.getShared().getSite(aName);
    if((site==null || !site.getExists()) && aURLString!=null) {
        if(site==null) site = WelcomePanel.getShared().createSite(aName, false);
        VersionControl.setRemoteURLString(site, aURLString);
        VersionControl vc = VersionControl.create(site);
        checkout(view, vc);
        return;
    }
    
    // If site still null complain and return
    if(site==null) {
        DialogBox.showErrorDialog(view, "Error Adding Project", "Project not found."); return; }

    // Add project for SnapKit        
    _proj.getProjectSet().addProject(aName);
    getAppPane().addSite(site);
}

/**
 * Load all remote files into project directory.
 */
public void checkout(View aView, VersionControl aVC)
{
    WebSite site = aVC.getSite();
        
    TaskRunner runner = new TaskRunnerPanel(_appPane.getUI(), "Checkout from " + aVC.getRemoteURLString()) {
        public Object run() throws Exception
        {
            aVC.checkout(this);
            return null;
        }
        public void success(Object aRes)
        {
            _proj.getProjectSet().addProject(site.getName());
            getAppPane().addSite(site);
        }
        public void failure(Exception e)
        {
            if(ClientUtils.setAccess(aVC.getRemoteSite())) checkout(aView, aVC);
            else if(new LoginPage().showPanel(_appPane.getUI(), aVC.getRemoteSite())) checkout(aView, aVC);
            else super.failure(e);
        }
    };
    runner.start();
}

/**
 * Build project.
 */
public void buildProjectLater(boolean doAddFiles)
{
    // If not root ProjectPane, forward on to it
    Project rootProj = _proj.getRootProject();
    ProjectPane rootProjPane = _proj!=rootProj? ProjectPane.get(rootProj.getSite()) : this;
    if(this!=rootProjPane) {
        rootProjPane.buildProjectLater(doAddFiles); return; }
    
    // If not already set, register for buildLater run
    if(_buildLaterRun!=null) return;
    runLater(_buildLaterRun = () -> buildProject(doAddFiles));
}

/**
 * Build project.
 */
public void buildProject(boolean doAddFiles)
{
    getBuildFilesRunner(doAddFiles);
    _buildLaterRun = null;
}

/**
 * Returns the build files runner.
 */
private synchronized BuildFilesRunner getBuildFilesRunner(boolean addBuildFiles)
{
    BuildFilesRunner bfr = _buildFilesRunner;
    if(bfr!=null && addBuildFiles) {
        bfr._addFiles = addBuildFiles; bfr = _buildFilesRunner; }
    if(bfr!=null) {
        bfr._runAgain = true; _proj.interruptBuild(); bfr = _buildFilesRunner; }
    if(bfr==null) {
        bfr = _buildFilesRunner = new BuildFilesRunner();
        _buildFilesRunner._addFiles = addBuildFiles;
        _buildFilesRunner.start();
    }
    return bfr;
}

/**
 * An Runner subclass to build project files in the background.
 */
public class BuildFilesRunner extends TaskRunner {

    // Whether to add files and run again
    boolean      _addFiles, _runAgain;

    /** BuildFiles. */
    public Object run()
    {
        if(_addFiles) { _addFiles = false; _projSet.addBuildFilesAll(); }
        _projSet.buildProjects(this);
        return true;
    }
    public void beginTask(final String aTitle, int theTotalWork)  { setActivity(aTitle); }
    public void finished()
    {
        boolean runAgain = _runAgain; _runAgain = false;
        if(runAgain) start();
        else _buildFilesRunner = null;
        setActivity("Build Completed");
        runLater(() -> handleBuildCompleted());
    }
    void setActivity(String aStr)  { if(_appPane!=null) _appPane.getBrowser().setActivity(aStr); }
    public void failure(final Exception e)
    {
        e.printStackTrace();
        runLater(() -> DialogBox.showExceptionDialog(null, "Build Failed", e));
        _runAgain = false;
    }
}

/**
 * Removes build files from the project.
 */
public void cleanProject()
{
    boolean old = setAutoBuildEnabled(false);
    _proj.cleanProject();
    setAutoBuildEnabled(old);
}

/**
 * Called when file added to project.
 */
void fileAdded(WebFile aFile)
{
    if(_proj.getBuildDir().contains(aFile)) return;
    _proj.fileAdded(aFile);
    if(_sitePane.isAutoBuild() && _sitePane.isAutoBuildEnabled()) buildProjectLater(false);
}

/**
 * Called when file removed from project.
 */
void fileRemoved(WebFile aFile)
{
    if(_proj.getBuildDir().contains(aFile)) return;
    _proj.fileRemoved(aFile);
    if(_sitePane.isAutoBuild() && _sitePane.isAutoBuildEnabled()) buildProjectLater(false);
}

/**
 * Called when file saved in project.
 */
void fileSaved(WebFile aFile)
{
    if(_proj.getBuildDir().contains(aFile)) return;
    _proj.fileSaved(aFile);
    if(_sitePane.isAutoBuild() && _sitePane.isAutoBuildEnabled()) buildProjectLater(false);
}

/**
 * Returns the list of jar paths.
 */
public String[] getJarPaths()  { return _proj.getClassPath().getLibPaths(); }

/**
 * Returns the selected JarPath.
 */
public String getSelectedJarPath()
{
    if(_jarPath==null && getJarPaths().length>0) _jarPath = getJarPaths()[0];
    return _jarPath;
}

/**
 * Sets the selected JarPath.
 */
public void setSelectedJarPath(String aJarPath)  { _jarPath = aJarPath; }

/**
 * Returns the list of dependent project paths.
 */
public String[] getProjectPaths()  { return _proj.getClassPath().getProjectPaths(); }

/**
 * Returns the selected Project Path.
 */
public String getSelectedProjectPath()
{
    if(_projPath==null && getProjectPaths().length>0) _projPath = getProjectPaths()[0];
    return _projPath;
}

/**
 * Sets the selected Project Path.
 */
public void setSelectedProjectPath(String aProjPath)  { _projPath = aProjPath; }

/**
 * Called when project breakpoint added.
 */
protected void notifyBreakpointAdded(Breakpoint aBP)
{
    // Tell active processes about breakpoint change
    for(RunApp rp : getAppPane().getProcPane().getProcs())
        rp.addBreakpoint(aBP);
}
    
/**
 * Called when project breakpoint removed.
 */
protected void notifyBreakpointRemoved(Breakpoint aBP)
{
    // Make current JavaPage.TextView resetLater
    WebPage page = getAppPane().getBrowser().getPage(aBP.getFile().getURL());
    if(page instanceof JavaPage)
        ((JavaPage)page).getTextView().repaintAll();
    
    // Tell active processes about breakpoint change
    for(RunApp rp : getAppPane().getProcPane().getProcs())
        rp.removeBreakpoint(aBP);
}
    
/**
 * Called when project BuildIssue added.
 */
protected void notifyBuildIssueAdded(BuildIssue aBI)
{
    // Make current JavaPage.TextView resetLater
    WebPage page = getAppPane().getBrowser().getPage(aBI.getFile().getURL());
    if(page instanceof JavaPage)
        ((JavaPage)page).getTextView().repaintAll();
    
    // Update FilesPane.FilesTree
    getAppPane().getFilesPane().updateFile(aBI.getFile());
}

/**
 * Called when project BuildIssue removed.
 */
protected void notifyBuildIssueRemoved(BuildIssue aBI)
{
    // Make current JavaPage.TextView resetLater
    WebPage page = getAppPane().getBrowser().getPage(aBI.getFile().getURL());
    if(page instanceof JavaPage)
        ((JavaPage)page).getTextView().repaintAll();

    // Update FilesPane.FilesTree
    getAppPane().getFilesPane().updateFile(aBI.getFile());
}

/**
 * Called when a build is completed.
 */
protected void handleBuildCompleted()
{
    // Get final error count and see if problems pane should show or hide
    int ecount = _proj.getRootProject().getBuildIssues().getErrorCount();
    if(ecount>0 && getAppPane().getSupportTrayIndex()!=0)
        getAppPane().setSupportTrayIndex(0);
    if(ecount==0 && _appPane.getSupportTrayIndex()==SupportTray.PROBLEMS_PANE)
        _appPane.setSupportTrayVisible(false);
}

/**
 * Initialize UI.
 */
protected void initUI()
{
    // Have Backspace and Delete remove selected Jar path
    addKeyActionEvent("DeleteAction", "DELETE");
    addKeyActionEvent("BackSpaceAction", "BACK_SPACE");
    enableEvents("JarPathsList", DragEvents);
    enableEvents("ProjectPathsList", MouseClicked);
}

/**
 * Respond to UI changes.
 */
public void respondUI(ViewEvent anEvent)
{
    // Handle ResetHomePageButton
    if(anEvent.equals("ResetHomePageButton"))
        _sitePane.setHomePageURLString(null);

    // Handle JarPathsList
    if(anEvent.equals("JarPathsList") && anEvent.isDragEvent()) {
        anEvent.acceptDrag(); //TransferModes(TransferMode.COPY);
        anEvent.consume();
        if(anEvent.isDragDropEvent()) {
            List <File> files = anEvent.getDropFiles(); if(files==null || files.size()==0) return;
            for(File file : files) { String path = file.getAbsolutePath(); //if(StringUtils.endsWithIC(path, ".jar"))
                _proj.getClassPath().addLibPath(path); }
            _sitePane.buildSite(false);
            anEvent.dropComplete();
        }
    }
    
    // Handle ProjectPathsList
    if(anEvent.equals("ProjectPathsList") && anEvent.getClickCount()>1) {
        DialogBox dbox = new DialogBox("Add Project Dependency"); dbox.setQuestionMessage("Enter Project Name:");
        String pname = dbox.showInputDialog(getUI(), null); if(pname==null || pname.length()==0) return;
        addProject(pname, null);
    }
    
    // Handle DeleteAction
    if(anEvent.equals("DeleteAction") || anEvent.equals("BackSpaceAction")) {
        if(getView("JarPathsList").isFocused())
            _proj.getClassPath().removeLibPath(getSelectedJarPath());
        else if(getView("ProjectPathsList").isFocused())
            _proj.getProjectSet().removeProject(getSelectedProjectPath());
    }
}

/**
 * Returns the ProjectPane for a site.
 */
public synchronized static ProjectPane get(WebSite aSite)
{
    return (ProjectPane)aSite.getProp(ProjectPane.class.getName());
}

}