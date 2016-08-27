package snap.app;
import java.util.*;
import snap.util.*;
import snap.view.*;
import snap.web.*;

/**
 * An implementation of a panel to manage/open user Snap sites (projects).
 */
public class WelcomePanel extends ViewOwner {

    // The list of sites
    List <WebSite>          _sites;

    // The selected site
    WebSite                 _selectedSite;
    
    // The Runnable to be called when app quits
    Runnable                _onQuit;

    // The shared instance
    static WelcomePanel     _shared;

/**
 * Returns the shared instance.
 */
public static WelcomePanel getShared()
{
    if(_shared!=null) return _shared;
    return _shared!=null? _shared : (_shared = new WelcomePanel());
}

/**
 * Shows the welcome panel.
 */
public void showPanel()
{
    getUI(); // This is bogus - if this isn't called, WindowView gets reset
    getWindow().setVisible(true);
    resetLater();
}

/**
 * Hides the welcome panel.
 */
public void hide()
{
    // Hide window and stop animation
    getWindow().setVisible(false);
    
    // Write current list of sites, flush prefs
    writeSites();
    PrefsUtils.flush();
}

/**
 * Returns the number of site.
 */
public int getSiteCount()  { return getSites().size(); }

/**
 * Returns the site at given index.
 */
public WebSite getSite(int anIndex)  { return getSites().get(anIndex); }

/**
 * Returns the list of sites.
 */
public List <WebSite> getSites()  { return _sites!=null? _sites : (_sites=readSites()); }

/**
 * Adds a site.
 */
public void addSite(WebSite aSite)  { addSite(aSite, getSiteCount()); }

/**
 * Adds a site at given index.
 */
public void addSite(WebSite aSite, int anIndex)  { getSites().add(anIndex, aSite); }

/**
 * Removes a site at given index.
 */
public WebSite removeSite(int anIndex)  { return getSites().remove(anIndex); }

/**
 * Removes a given site from sites list.
 */
public int removeSite(WebSite aSite)
{
    int index = ListUtils.indexOfId(getSites(), aSite);
    if(index>=0) removeSite(index);
    return index;
}

/**
 * Returns a site for given URL or name, if available.
 */
public WebSite getSite(String aName)
{
    for(WebSite site : getSites())
        if(site.getURL().getString().equalsIgnoreCase(aName))
            return site;
    for(WebSite site : getSites())
        if(site.getName().equalsIgnoreCase(aName))
            return site;
    return null;
}

/**
 * Returns the selected site.
 */
public WebSite getSelectedSite()  { return _selectedSite; }

/**
 * Sets the selected site.
 */
public void setSelectedSite(WebSite aSite)  { _selectedSite = aSite; }

/**
 * Returns the list of selected sites.
 */
public List <WebSite> getSelectedSites() { return _selectedSite!=null? Arrays.asList(_selectedSite) : new ArrayList(); }

/**
 * Returns a list of selected site names.
 */
public List <String> getSelectedNames()
{
    List names = new ArrayList();
    for(WebSite site : getSelectedSites()) names.add(site.getName());
    return names;
}

/**
 * Returns the Runnable to be called to quit app.
 */
public Runnable getOnQuit()  { return _onQuit; }

/**
 * Sets the Runnable to be called to quit app.
 */
public void setOnQuit(Runnable aRunnable)  { _onQuit = aRunnable; }

/**
 * Called to quit app.
 */
public void quitApp()
{
    hide();
    _onQuit.run();
}

/**
 * Reads sites from <SNAP_HOME>/UserLocal.settings.
 */
protected List <WebSite> readSites()
{
    // Get site names and create sites.
    _sites = new ArrayList();
    Settings settings = ClientUtils.getUserLocalSettings();
    List <String> purls = settings.getList("Projects");
    if(purls==null) purls = settings.getList("SnapSites"); if(purls==null) return _sites;
        
    // Get site from string
    for(String purl : purls) {
        if(purl.indexOf(':')<0) purl = "local:/" + purl; // Turn names into local sites
        WebSite site = WebURL.getURL(purl).getAsSite();
        _sites.add(site);
    }
    
    // Get Selected Sites
    List <String> spurls = settings.getList("SelectedSites", true);
    for(String spurl : spurls) { WebSite site = getSite(spurl);
        if(site!=null)
            _selectedSite = site; break; } // _selectedSites.add(site);
    
    // Return sites
    return _sites;
}

/**
 * Saves sites to <SNAP_HOME>/UserLocal.settings.
 */
protected void writeSites()
{
    // Move selected sites to front of the list
    List <WebSite> selectedSites = getSelectedSites();
    for(int i=0, iMax=selectedSites.size(); i<iMax; i++) { WebSite site = selectedSites.get(i);
        if(site!=getSite(i) && ListUtils.removeId(_sites, site)>=0) _sites.add(i, site); }

    // Put Site URLs
    List urls = new ArrayList(); for(WebSite site : getSites()) urls.add(getSimpleURLString(site));
    ClientUtils.getUserLocalSettings().put("Projects", urls.size()>0? urls : null);
    
    // Put SelectedSites URLs
    List surls = new ArrayList(); for(WebSite site : getSelectedSites()) surls.add(getSimpleURLString(site));
    ClientUtils.getUserLocalSettings().put("SelectedSites", surls.size()>0? surls : null);
    ClientUtils.saveUserLocalSettings();
}

/**
 * Returns the simple URL for a site (just the name if local).
 */
private String getSimpleURLString(WebSite aSite)
{
    return aSite.getURL().getScheme().equals("local")? aSite.getName() : aSite.getURLString();
}

/**
 * Initialize UI panel.
 */
protected void initUI()
{
    // Add WelcomePaneAnim node
    WelcomePanelAnim anim = new WelcomePanelAnim();
    getUI(ChildView.class).addChild(anim.getUI()); anim.getUI().getAnimator(true).play();
    
    // Enable SitesTable MouseClicked
    TableView sitesTable = getView("SitesTable", TableView.class);
    sitesTable.setRowHeight(24); //sitesTable.setStyle(new Style().setFontSize(10).toString());
    enableEvents(sitesTable, MouseClicked);
    
    // Set preferred size
    getUI().setPrefSize(400,480);
    
    // Configure Window: Add WindowListener to indicate app should exit when close button clicked
    WindowView win = getWindow(); win.setTitle("Welcome"); win.setResizable(false);
    enableEvents(win, WinClosing);
    getView("OpenButton", Button.class).setDefaultButton(true);
    
    // Register buttons for MouseClicked so we can look for alt-Down click
    enableEvents("NewButton", MouseClicked);
    enableEvents("OpenButton", MouseClicked);
    enableEvents("RemoveButton", MouseClicked);
}

/**
 * Resets UI.
 */
public void resetUI()
{
    // Update OpenButton, RemoveButton
    setViewEnabled("OpenButton", getSelectedSites().size()>0);
    setViewEnabled("RemoveButton", getSelectedSites().size()>0);
}

/**
 * Responds to UI changes.
 */
public void respondUI(ViewEvent anEvent)
{
    // Handle SitesTable double-click
    if(anEvent.equals("SitesTable") && anEvent.getClickCount()>1)
        if(getView("OpenButton", Button.class).isEnabled()) {
            hide();
            openSites();
        }
    
    // Handle NewButton
    if(anEvent.equals("NewButton")) {
        if(anEvent.isMouseClicked())  { if(anEvent.isAltDown()) handleNewButtonAlt(); return; }
        createNewSite();
    }
    
    // Handle OpenButton
    if(anEvent.equals("OpenButton")) {
        if(anEvent.isMouseClicked()) { if(anEvent.isAltDown()) handleOpenButtonAlt(); return; }
        hide();
        openSites();
    }
    
    // Handle RemoveButton
    if(anEvent.equals("RemoveButton")) {
        if(anEvent.isMouseClicked())  { if(anEvent.isAltDown()) handleRemoveButtonAlt(); return; }
        showRemoveSitePanel();
    }

    // Handle QuitButton
    if(anEvent.equals("QuitButton"))
        quitApp();
        
    // Handle WinClosing
    if(anEvent.isWinClosing())
        quitApp();
}

/**
 * Creates a new Site.
 */
protected void createNewSite()
{
    // Get name for new project/site
    DialogBox dbox = new DialogBox("New Project Panel"); dbox.setMessage("Enter name of new project");
    String name = dbox.showInputDialog(getUI(), "Untitled"); if(name==null) return;

    // If name already exists, select and return
    if(getSite(name)!=null) {
        setSelectedSite(getSite(name)); return; }
    
    // Create and configure new site
    String urls = name.indexOf(':')<0? "local:/" + name : name;
    WebSite site = WebURL.getURL(urls).getAsSite();
   
    // Add new site
    addNewSite(site);
}

/**
 * Adds a new site to welcome panel.
 */
public WebSite addNewSite(WebSite aSite)
{
    addSite(aSite);
    setSelectedSite(aSite);
    writeSites();
    resetLater();
    return aSite;
}

/**
 * Opens selected sites.
 */
public void openSites()
{
    // Create AppPane and add selected sites
    AppPane appPane = new AppPane();
    for(WebSite site : getSelectedSites())
        appPane.addSite(site);
        
    // Show AppPane
    appPane.show();
}

/**
 * Shows the remove site panel.
 */
public void showRemoveSitePanel()
{
    // Get selected site (if null, just return)
    WebSite site = getSelectedSite(); if(site==null) return;
    
    // Give the user a chance to bail (just return if canceled or closed)
    String msg = "Are you sure you want to remove the currently selected project?";
    DialogBox dbox = new DialogBox("Remove Project"); dbox.setMessage(msg);
    if(!dbox.showConfirmDialog(getUI())) return;
    
    // Give the option to not delete resources (just return if closed)
    msg = "Also delete local project files and sandbox?";
    dbox = new DialogBox("Delete Local Project Files"); dbox.setMessage(msg);
    boolean deleteLocal = dbox.showConfirmDialog(getUI());
    
    // If requested, delete site files and sandbox (if "local" site)
    if(deleteLocal && site.getURL().getScheme().equals("local"))
        SitePane.get(site, true).deleteSite(getUI());
    
    // Get site index and select next index
    int index = ListUtils.indexOfId(getSites(), site);
    
    // Remove site
    removeSite(site);
    
    // Reset SelectedSite
    int sindex = Math.min(index, getSiteCount()-1);
    setSelectedSite(sindex>=0 && sindex<getSiteCount()? getSite(sindex) : null);
    
    // Reset ui
    resetLater();
}

/** Called when NewButton hit with Alt down. */
void handleNewButtonAlt()
{
    AppPane apane = new AppPane();
    WebSite site = WebURL.getURL("file:/Temp/Cust").getAsSite();
    apane.addSite(site);
    apane.show();
    hide();
}

/** Called when RemoveButton hit with Alt down. */
void handleRemoveButtonAlt()
{
    WebSite site = getSelectedSite(); if(site==null) return;
    String msg = "Delete backend resources for Snap site?";
    DialogBox dbox = new DialogBox("Delete Snap Site Resources"); dbox.setMessage(msg);
    if(!dbox.showConfirmDialog(getUI())) return;
    try { site.deleteSite(); }
    catch(Exception e) { throw new RuntimeException(e); }
}

/** Open a file viewer site. */
void handleOpenButtonAlt()
{
    DialogBox dbox = new DialogBox("Open File Viewer"); dbox.setQuestionMessage("Enter path:");
    String path = PrefsUtils.getPrefs().get("SnapFileViewerPath", System.getProperty("user.home"));
    path = dbox.showInputDialog(getUI(), path); if(path==null) return;
    WebURL url = WebURL.getURL(path); if(url==null || url.getFile()==null) return;
    PrefsUtils.getPrefs().put("SnapFileViewerPath", path);
    AppPane apane = new AppPane();
    WebSite site = url.getAsSite();
    apane.addSite(site);
    apane.show();
    hide();
}

/**
 * A viewer owner to load/view WelcomePanel animation from WelcomePanelAnim.rpt.
 */
private static class WelcomePanelAnim extends ViewOwner {  //RMViewerOwner {

    /** Needed since file is rpt instead of jfx. */
    public Object getUISource() { return WebURL.getURL(getClass(), "WelcomePanelAnim.rpt"); }

    /** Initialize some fields. */
    protected void initUI()
    {
        setViewText("BuildText", "Build: " + SnapUtils.getBuildInfo());
        setViewText("JVMText", "JVM: " + System.getProperty("java.runtime.version"));
        DocView doc = getUI(DocView.class);
        PageView page = (PageView)doc.getPage();
        page.setEffect(null); page.setBorder(null);
    }
}

}