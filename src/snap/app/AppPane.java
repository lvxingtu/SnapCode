package snap.app;
import java.util.*;
import snap.app.SearchPane;
import snap.project.*;
import snap.util.*;
import snap.view.*;
import snap.viewx.*;
import snap.web.*;

/**
 * The main view class for Projects.
 */
public class AppPane extends ViewOwner implements PropChangeListener, DeepChangeListener {

    // The list of sites
    List <WebSite>                _sites = new ArrayList();
    
    // The AppPaneToolBar
    AppPaneToolBar                _toolBar = new AppPaneToolBar(this);
    
    // The FilesPane
    AppFilesPane                  _filesPane = new AppFilesPane(this);
    
    // The ProcPane manages run/debug processes
    ProcPane                      _procPane = new ProcPane(this);
    
    // The AppBroswer for displaying editors
    AppBrowser                    _browser;
    
    // The pane that the browser sits in
    SplitView                     _browserBox;
    
    // The SupportTray
    SupportTray                   _supportTray = new SupportTray(this);
    
    // The Problems pane
    BuildIssuesPane               _problemsPane = new BuildIssuesPane(this);

    // The RunConsole
    RunConsole                    _runConsole = new RunConsole(this);
    
    // The DebugVarsPane
    DebugVarsPane                 _debugVarsPane = new DebugVarsPane(this);
    
    // The DebugExprsPane
    DebugExprsPane                _debugExprsPane = new DebugExprsPane(this);
    
    // The BreakpointsPanel
    BreakpointsPanel              _breakpointsPanel = new BreakpointsPanel(this);
    
    // The SearchPane
    SearchPane                    _searchPane = new SearchPane(this);
    
    // The currently open AppPane
    static AppPane                _openAppPane;

/**
 * Returns the browser.
 */
public AppBrowser getBrowser()  { return _browser; }

/**
 * Returns the browser box.
 */
public SplitView getBrowserBox()  { return _browserBox; }

/**
 * Returns the toolbar.
 */
public AppPaneToolBar getToolBar()  { return _toolBar; }

/**
 * Returns the files pane.
 */
public AppFilesPane getFilesPane()  { return _filesPane; }

/**
 * Returns the processes pane.
 */
public ProcPane getProcPane()  { return _procPane; }

/**
 * Returns whether SupportTray is visible.
 */
public boolean isSupportTrayVisible()  { return getBrowserBox().getItemCount()>1; }

/**
 * Sets SupportTray visible.
 */
public void setSupportTrayVisible(boolean aValue)
{
    // If value already set, or if asked to close ExplicitlyOpened SupportTray, just return
    if(aValue==isSupportTrayVisible() || !aValue && _supportTray.isExplicitlyOpened()) return;
    
    // Get SupportTray UI and SplitView
    View supTrayUI = _supportTray.getUI();
    SplitView spane = getBrowserBox();
    
    // Add/remove SupportTrayUI with animator
    if(aValue) spane.addChildWithAnim(supTrayUI, 240);
    else spane.removeChildWithAnim(supTrayUI);
    
    // Update ShowTrayButton
    setViewText("ShowTrayButton", aValue? "Hide Tray" : "Show Tray");
}

/**
 * Returns the SupportTray index.
 */
public int getSupportTrayIndex()  { return isSupportTrayVisible()? _supportTray.getSelectedIndex() : -1; }

/**
 * Sets SupportTray visible to given index.
 */
public void setSupportTrayIndex(int anIndex) { setSupportTrayVisible(true); _supportTray.setSelectedIndex(anIndex); }

/**
 * Returns the number of sites.
 */
public int getSiteCount()  { return _sites.size(); }

/**
 * Returns the individual site at the given index.
 */
public WebSite getSite(int anIndex)  { return _sites.get(anIndex); }

/**
 * Returns the list of sites.
 */
public List <WebSite> getSites()  { return _sites; }

/**
 * Adds a site to sites list.
 */
public void addSite(WebSite aSite)
{
    // Create project for site
    Project proj = Project.get(aSite, true);

    // Add site
    _sites.add(getSiteCount(), aSite);  // Add site
    SitePane.get(aSite, true).setAppPane(this);
    aSite.addDeepChangeListener(this);
    
    // Add dependent sites
    for(Project p : proj.getProjects())
        addSite(p.getSite());
}

/**
 * Removes a site from sites list.
 */
public void removeSite(WebSite aSite)
{
    _sites.remove(aSite);
    aSite.removeDeepChangeListener(this);
}

/**
 * Shows the AppPane window.
 */
public void show()
{
    // Set AppPane as OpenSite and show window
    getUI(); _openAppPane = this;
    getWindow().setSaveName("AppPane");
    getWindow().setSaveSize(true);
    getWindow().setVisible(true);
    
    // Open site and show home page
    SitePane.get(getSite(0)).openSite();
    showHomePage();
}

/**
 * Close this AppPane.
 */
public void hide()
{
    // Flush and refresh sites
    for(WebSite site : getSites()) {
        SitePane.get(site).closeSite();
        try { site.flush(); }
        catch(Exception e) { e.printStackTrace(); throw new RuntimeException(e); }
        site.resetFiles();
    }
    _openAppPane = null;
}

/**
 * Returns the current open AppPane.
 */
public static AppPane getOpenAppPane()  { return _openAppPane; }

/**
 * Shows the home page.
 */
public void showHomePage()  { getBrowser().setURL(getHomePageURL()); }

/**
 * Returns the HomePageURL.
 */
public WebURL getHomePageURL()
{
    WebURL url = SitePane.get(getSite(0)).getHomePageURL();
    return url!=null? url : getFilesPane().getHomePageURL();
} 

/**
 * Returns the selected file.
 */
public WebFile getSelectedFile()  { return _sf; } WebFile _sf;

/**
 * Sets the selected site file.
 */
public void setSelectedFile(WebFile aFile)
{
    // If file already set, just return
    if(aFile==null || aFile==getSelectedFile()) return;
    _sf = aFile;
    
    // Set selected file and update tree
    if(_sf!=null && _sf.isFile() || _sf.isRoot())
        getBrowser().setFile(_sf);
    _filesPane.resetLater();
}

/**
 * Returns the selected directory.
 */
public WebFile getSelectedDir()  { WebFile sf = getSelectedFile(); return sf.isDir()? sf : sf.getParent(); }

/**
 * Returns the selected site.
 */
public WebSite getSelectedSite()
{
    WebFile file = getSelectedFile();
    WebSite site = file!=null? file.getSite() : null;
    if(!ListUtils.containsId(getSites(), site)) site = getSite(0);
    return site;
}

/**
 * Returns the build directory.
 */
public WebFile getBuildDir()
{
    WebSite site = getSelectedSite();
    Project proj = site!=null? Project.get(site) : null;
    return proj!=null? proj.getBuildDir() : null;
}

/**
 * Listens to changes to sites.
 */
public void propertyChange(PropChange anEvent)
{
    // Get source and property name
    Object source = anEvent.getSource(); String pname = anEvent.getPropertyName();
    
    // Handle WebPage.Modified change
    if(source instanceof WebPage && pname.equals("Modified"))
        resetLater();
    
    // Handle WebBrowser change
    else if(source instanceof WebBrowser)
        resetLater();
}

/**
 * Catch changes to files.
 */
public void deepChange(PropChangeListener aSource, PropChange anEvent)
{
    // Get source and property name
    Object source = anEvent.getSource(); String pname = anEvent.getPropertyName();
    
    // If WebFile, update FilesPane.TreeView
    if(source instanceof WebFile) { WebFile file = (WebFile)source;
        if(file.getExists())
            _filesPane.updateFile(file); }
}

/**
 * Creates the UI.
 */
protected View createUI()
{
    View view = super.createUI(); view.setGrowHeight(true); view.setPrefSize(958, 684);
    VBox vbox = new VBox(); vbox.setFillWidth(true);
    vbox.setChildren(_toolBar.getUI(), view);
    return vbox;
}

/**
 * Initializes UI panel.
 */
protected void initUI()
{
    // Get AppBrowser
    _browser = getView("Browser", AppBrowser.class);
    _browser.setAppPane(this);
    _browser.addPropChangeListener(this);
    
    // Add the FilesPane, ProcPane
    SplitView fspane = getView("FilesSplitView", SplitView.class); fspane.setPrefWidth(220);
    fspane.setBorder(null);
    View filesPaneUI = _filesPane.getUI(); filesPaneUI.setGrowHeight(true);
    View procPaneUI = _procPane.getUI(); procPaneUI.setPrefHeight(250);
    fspane.setChildren(filesPaneUI, procPaneUI);
    
    // Get browser box
    _browserBox = getView("BrowserBox", SplitView.class);
    _browserBox.setGrowWidth(true); _browserBox.setBorder(null);
    for(View c : _browserBox.getChildren()) c.setBorder(null);
    _browserBox.getChild(0).setGrowHeight(true); // So support tray has constant size
    
    // Add key binding to OpenMenuItem and CloseWindow
    addKeyActionEvent("OpenMenuItem", "meta O");
    addKeyActionEvent("CloseFileAction", "meta W");

    // Configure Window
    getWindow().setTitle("SnapCode Project");
    getWindow().setMenuBar(getMenuBar());
    
    // Register for WelcomePanel on close
    enableEvents(getWindow(), WinClosing);
}

/**
 * Resets UI panel.
 */
public void resetUI()
{
    // Reset window title
    WebPage page = getBrowser().getPage();
    getWindow().setTitle(page!=null? page.getTitle() : "SnapCode");

    // Set ActivityText, StatusText
    setViewText("ActivityText", getBrowser().getActivity());
    setViewText("StatusText", getBrowser().getStatus());
    
    // Update ProgressBar
    ProgressBar pb = getView("ProgressBar", ProgressBar.class); boolean loading = getBrowser().isLoading();
    if(loading && !pb.isVisible()) { pb.setVisible(true); pb.setProgress(-1); }
    else if(!loading && pb.isVisible()) { pb.setProgress(0); pb.setVisible(false); }
    
    // Reset FilesPane and SupportTray
    _filesPane.resetLater();
    _procPane.resetLater();
    _supportTray.resetLater();
}

/**
 * Responds to UI panel controls.
 */
public void respondUI(ViewEvent anEvent)
{
    // Handle OpenMenuItem
    if(anEvent.equals("OpenMenuItem"))
        getToolBar().selectSearchText();

    // Handle QuitMenuItem
    if(anEvent.equals("QuitMenuItem"))
        WelcomePanel.getShared().quitApp();

    // Handle NewFileMenuItem, NewFileButton
    if(anEvent.equals("NewFileMenuItem") || anEvent.equals("NewFileButton"))
        showNewFilePanel();

    // Handle CloseMenuItem, CloseFileAction
    if(anEvent.equals("CloseMenuItem") || anEvent.equals("CloseFileAction"))
        getToolBar().removeOpenFile(getSelectedFile());
    
    // Handle ShowTrayButton
    if(anEvent.equals("ShowTrayButton")) { boolean isVisible = isSupportTrayVisible();
         _supportTray.setExplicitlyOpened(!isVisible); setSupportTrayVisible(!isVisible); }
    
    // Handle ProcessesList
    if(anEvent.equals("ProcessesList"))
        setSupportTrayIndex(2);
    
    // Handle ShowJavaHomeMenuItem
    if(anEvent.equals("ShowJavaHomeMenuItem")) {
        String java = System.getProperty("java.home");
        FileUtils.openFile(java);
    }
    
    // Handle WinClosing
    if(anEvent.isWinClosing()) {
        hide(); runLater(() -> { PrefsUtils.flush(); WelcomePanel.getShared().showPanel(); }); }
}

/**
 * Returns the MenuBar.
 */
public MenuBar getMenuBar()  { return _mbar!=null? _mbar : (_mbar=createMenuBar()); } MenuBar _mbar;

/**
 * Creates the MenuBar.
 */
protected MenuBar createMenuBar()
{
    MenuBar mbar = new MenuBar();
    Menu fileMenu = createMenu("FileMenu", "File"); mbar.addMenu(fileMenu);
    fileMenu.addItem(createMenuItem("NewMenuItem", "New", "Shortcut+N"));
    fileMenu.addItem(new MenuItem()); //SeparatorMenuItem
    fileMenu.addItem(createMenuItem("OpenMenuItem", "Open", "Shortcut+O"));
    fileMenu.addItem(createMenuItem("CloseMenuItem", "Close", "Shortcut+W"));
    fileMenu.addItem(createMenuItem("SaveMenuItem", "Save", "Shortcut+S"));
    fileMenu.addItem(createMenuItem("SaveAsMenuItem", "Save As...", "Shortcut+Shift+S"));
    fileMenu.addItem(createMenuItem("RevertMenuItem", "Revert to Saved", "Shortcut+U"));
    fileMenu.addItem(createMenuItem("QuitMenuItem", "Quit", "Shortcut+Q"));
    Menu editMenu = createMenu("EditMenu", "Edit"); mbar.addMenu(editMenu);
    editMenu.addItem(createMenuItem("UndoMenuItem", "Undo", "Shortcut+Z"));
    editMenu.addItem(createMenuItem("RedoMenuItem", "Redo", "Shortcut+Shift+Z"));
    editMenu.addItem(new MenuItem()); //SeparatorMenuItem
    editMenu.addItem(createMenuItem("CutMenuItem", "Cut", "Shortcut+X"));
    editMenu.addItem(createMenuItem("CopyMenuItem", "Copy", "Shortcut+C"));
    editMenu.addItem(createMenuItem("PasteMenuItem", "Paste", "Shortcut+V"));
    editMenu.addItem(createMenuItem("SelectAllMenuItem", "Select All", "Shortcut+A"));
    Menu helpMenu = createMenu("HelpMenu", "Help"); mbar.addMenu(helpMenu);
    helpMenu.addItem(createMenuItem("SupportPageMenuItem", "Support Page", null));
    helpMenu.addItem(createMenuItem("JavaDocMenuItem", "Tutorial", null));
    helpMenu.addItem(createMenuItem("ShowJavaHomeMenuItem", "Show Java Home", null));
    initUI(mbar);
    return mbar;
}

/**
 * Creates a JMenu for name and text.
 */
Menu createMenu(String aName, String theText)
{
    Menu m = new Menu(); m.setText(theText); m.setName(aName);
    //m.setStyle(new Style().setFontSize(14).setPadding(2,5,3,5).toString());
    return m;
}

/**
 * Creates a JMenuItem for name and text and key accelerator description.
 */
MenuItem createMenuItem(String aName, String theText, String aKey)
{
    MenuItem mi = new MenuItem(); mi.setText(theText); mi.setName(aName);
    if(aKey!=null) mi.setAccelerator(aKey);
    return mi;
}

/**
 * Returns the support tray.
 */
public SupportTray getSupportTray()  { return _supportTray; }

/**
 * Returns the problems pane.
 */
public BuildIssuesPane getProblemsPane()  { return _problemsPane; }

/**
 * Returns the RunConsole.
 */
public RunConsole getRunConsole()  { return _runConsole; }

/**
 * Returns the DebugVarsPane.
 */
public DebugVarsPane getDebugVarsPane()  { return _debugVarsPane; }

/**
 * Returns the DebugExprsPane.
 */
public DebugExprsPane getDebugExprsPane()  { return _debugExprsPane; }

/**
 * Returns the BreakpointsPanel.
 */
public BreakpointsPanel getBreakpointsPanel()  { return _breakpointsPanel; }

/**
 * Returns the SearchPane.
 */
public SearchPane getSearchPane()  { return _searchPane; }

/**
 * Saves any unsaved files.
 */
public int saveFiles()  { return _filesPane.saveFiles(getSelectedSite().getRootDir(), true); }

/**
 * Runs a panel for a new file.
 */
public void showNewFilePanel()  { _filesPane.showNewFilePanel(); }

}