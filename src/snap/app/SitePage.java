package snap.app;
import snap.view.*;
import snap.viewx.WebPage;

/**
 * A WebPage subclass for a site's root WebFile.
 */
public class SitePage extends WebPage {

/**
 * Initialize UI panel.
 */
protected View createUI()
{
    // Create TabView
    TabView _tview = new TabView();
    
    // Get SitePane
    SitePane sitePane = SitePane.get(getSite());
    
    // Add ProjectPane
    ProjectPane projPane = sitePane.getProjPane();
    if(projPane!=null)
        _tview.addTab("Settings", projPane.getUI()); //tab.setTooltip(new Tooltip("Project Settings"));
    
    // Add VersionControlPane
    VcsPane vcp = sitePane.getVersionControlPane();
    if(vcp!=null) {
        String title = sitePane.getVersionControlPane() instanceof VcsPaneGit? "Git" : "Versioning";
        _tview.addTab(title, vcp.getUI()); //tab.setTooltip(new Tooltip("Manage Remote Site"));
    }
        
    // Add console pane and return
    AppConsole consolePane = sitePane.getConsolePane();
    _tview.addTab("Console", consolePane.getUI());  //tab.setTooltip(new Tooltip("Console Text"));
    
    // Add BuildPane
    BuildPane buildPane = sitePane._buildPane;
    _tview.addTab("Build Dir", buildPane.getUI());
    
    // Add HttpServerPane
    HttpServerPane httpServPane = sitePane._httpServerPane;
    _tview.addTab("HTTP-Server", httpServPane.getUI());
    
    // Return TabView
    return _tview;
}

/**
 * Override to provide better title.
 */
public String getTitle()  { return getURL().getSite().getName() + " - Site Settings"; }

}