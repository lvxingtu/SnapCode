package snap.app;
import snap.project.*;
import snap.util.*;
import snap.view.*;
import snap.web.*;

/**
 * A class to manage UI aspects of a WebSite for app.
 */
public class SitePane extends ViewOwner implements DeepChangeListener {

    // The AppPane that owns this SitePane
    AppPane            _appPane;

    // The WebSite
    WebSite            _site;
    
    // The ConsolePane
    AppConsole         _consolePane = new AppConsole();

    // The ProjectPane
    ProjectPane        _projPane;
    
    // The VersionControl pane
    VcsPane            _vcp;
    
/**
 * Creates a new SitePane for given site.
 */
protected SitePane(WebSite aSite)
{
    _site = aSite;
    _site.addDeepChangeListener(this);
    
    // Set ProjectPane
    _projPane = new ProjectPane(this);
    
    // Set VersionControlPane
    String urls = getRemoteURLString();
    _vcp = VersionControl.get(_site) instanceof VersionControlGit? new VcsPaneGit(this) : new VcsPane(this);
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
    _appPane = anAP; _consolePane._appPane = anAP;
    if(_projPane!=null) _projPane.setAppPane(anAP);
    if(_vcp!=null) _vcp.setAppPane(anAP);
}

/**
 * Returns the site.
 */
public WebSite getSite()  { return _site; }

/**
 * Returns the ConsolePane.
 */
public AppConsole getConsolePane()  { return _consolePane; }

/**
 * Returns the ProjectPane for this site.
 */
public ProjectPane getProjPane()  { return _projPane; }

/**
 * Returns the VersionControlPane.
 */
public VcsPane getVersionControlPane()  { return _vcp; }

/**
 * Returns the HomePageURL.
 */
public WebURL getHomePageURL()
{
    if(_hpu==null) {
        String hpu = getHomePageURLString(); if(hpu!=null) _hpu = getSite().getURL(hpu); }
    return _hpu;
} WebURL _hpu;

/**
 * Sets the HomePageURL.
 */
public void setHomePageURL(WebURL aURL)  { setHomePageURLString(aURL!=null? aURL.getString() : null); _hpu = aURL; }

/**
 * Returns the HomePageURL.String.
 */
public String getHomePageURLString()  { return _homePageURLS; } String _homePageURLS;

/**
 * Sets the HomePageURL.
 */
public void setHomePageURLString(String aURL)  { _homePageURLS = aURL; _hpu = null; }

/**
 * Returns the RemoteURL string.
 */
public String getRemoteURLString()  { return VersionControl.getRemoteURLString(_site); }

/**
 * Sets the RemoteURL string.
 */
public void setRemoteURLString(String urls)
{
    TabView tpane = _vcp.getUI().getParent(TabView.class);
    int index = -1; for(int i=0;i<tpane.getTabCount();i++) if(tpane.getTabContent(i)==_vcp.getUI()) index = i;
    
    // Deactivate Version control pane and re-open site
    _vcp.deactivate();
    VersionControl.setRemoteURLString(_site, urls);
    _vcp = VersionControl.get(_site) instanceof VersionControlGit? new VcsPaneGit(this) : new VcsPane(this);
    if(_appPane!=null) _vcp.setAppPane(_appPane);
    _vcp.openSite();

    // Reset UI
    //TabView tpane = _vcp.getUI().getParent(TabView.class);
    //int index = -1; for(int i=0;i<tpane.getTabCount();i++) if(tpane.getTabContent(i)==_vcp.getUI()) index = i;
    tpane.setTabContent(_vcp.getUI(), index);
}

/**
 * Returns whether to automatically build files when changes are detected.
 */
public boolean isAutoBuild()  { return _projPane!=null && _projPane.isAutoBuild(); }

/**
 * Sets whether to automatically build files when changes are detected.
 */
public void setAutoBuild(boolean aValue)  { if(_projPane!=null) _projPane.setAutoBuild(aValue); }

/**
 * Returns whether to project AutoBuild has been disabled (possibly for batch processing).
 */
public boolean isAutoBuildEnabled()  { return _projPane!=null && _projPane.isAutoBuildEnabled(); }

/**
 * Sets whether to project AutoBuild has been disabled (possibly for batch processing).
 */
public boolean setAutoBuildEnabled(boolean aFlag)  { return _projPane!=null && _projPane.setAutoBuildEnabled(aFlag); }

/**
 * Returns whether app should use visual SnapCode Java editor by default.
 */
public boolean getUseSnapEditor()  { return _useSnapEditor; } boolean _useSnapEditor;

/**
 * Sets whether app should use visual SnapCode Java editor by default.
 */
public void setUseSnapEditor(boolean aValue)  { _useSnapEditor = aValue; }

/**
 * Opens the Site.
 */
public void openSite()
{
    // Activate VersionControlPane
    if(_vcp!=null)
        _vcp.openSite();
    
    // Activate ProjectPane
    if(_projPane!=null)
        _projPane.openSite();
}

/**
 * Closes the site.
 */
public void closeSite()
{
    _site.removeDeepChangeListener(this);
    _site.setProp(SitePane.class.getName(), null);
    _appPane = null; _site = null; _consolePane = null; _projPane = null; _vcp = null;
}

/**
 * Deletes a site.
 */
public void deleteSite(View aView)
{
    if(_projPane!=null)
        _projPane.deleteProject(aView);
    else {
        try { _site.deleteSite(); }
        catch(Exception e) { e.printStackTrace(); DialogBox.showExceptionDialog(null, "Delete Site Failed", e); }
    }
}

/**
 * Builds the site (if site has project).
 */
public void buildSite(boolean doAddFiles)
{
    if(_projPane!=null)
        _projPane.buildProjectLater(doAddFiles);
}

/**
 * Removes build files from site.
 */
public void cleanSite()
{
    if(_projPane!=null)
        _projPane.cleanProject();
}

/**
 * Returns whether given file is a hidden file.
 */
public boolean isHiddenFile(WebFile aFile)
{
    if(aFile==_projPane.getProject().getBuildDir())
        return true;
    if(aFile.getPath().startsWith("/.git"))
        return true;
    return false;
}

/**
 * Implement to listen to Site File changes.
 */
public void deepChange(PropChangeListener aSource, PropChange anEvent)
{
    // Get source and property name
    Object source = anEvent.getSource(); String pname = anEvent.getPropertyName();
    
    // Handle WebFile
    if(source instanceof WebFile) { WebFile file = (WebFile)source;
        
        // Handle Exists property: Call fileAdded or fileSaved
        if(pname==WebFile.Exists_Prop) {
            if((Boolean)anEvent.getNewValue()) fileAdded(file);
            else fileRemoved(file);
        }
        
        // Handle ModifedTime property: Call file saved
        if(pname==WebFile.ModifiedTime_Prop && file.getExists()) fileSaved(file);
    }
}

/**
 * Called when file added to project.
 */
void fileAdded(WebFile aFile)
{
    if(_projPane!=null) _projPane.fileAdded(aFile);
    if(_vcp!=null) _vcp.fileAdded(aFile);
}

/**
 * Called when file removed from project.
 */
void fileRemoved(WebFile aFile)
{
    if(_projPane!=null) _projPane.fileRemoved(aFile);
    if(_vcp!=null) _vcp.fileRemoved(aFile);
}

/**
 * Called when file saved in project.
 */
void fileSaved(WebFile aFile)
{
    if(_projPane!=null) _projPane.fileSaved(aFile);
    if(_vcp!=null) _vcp.fileSaved(aFile);
}

/**
 * Returns the site pane for a site.
 */
public static SitePane get(WebSite aSite)  { return get(aSite, false); }

/**
 * Returns the site pane for a site.
 */
public synchronized static SitePane get(WebSite aSite, boolean doCreate)
{
    SitePane sp = (SitePane)aSite.getProp(SitePane.class.getName());
    if(sp==null && doCreate) aSite.setProp(SitePane.class.getName(), sp=new SitePane(aSite));
    return sp;
}

}