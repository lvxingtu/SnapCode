package snap.playground;
import snap.view.*;

/**
 * A class to hold TabView for ProblemsPane, RunConsole, DebugPane.
 */
public class PGTabPane extends ViewOwner {

    // The Playground
    Playground     _appPane;
    
    // The tabview
    TabView        _tview;
    
    // The list of tab owners
    ViewOwner      _tabOwners[];
    
    // Whether tray was explicitly opened
    boolean        _explicitlyOpened;
    
    // Constants for tabs
    public static final int PROBLEMS_PANE = 0;
    public static final int RUN_PANE = 1;
    public static final int DEBUG_PANE_VARS = 2;
    public static final int DEBUG_PANE_EXPRS = 3;
    public static final int BREAKPOINTS_PANE = 4;
    public static final int SEARCH_PANE = 5;
    
/**
 * Creates a new PGTabPane for given AppPane.
 */
public PGTabPane(Playground anAppPane)  { _appPane = anAppPane; }

/**
 * Returns the selected index.
 */
public int getSelectedIndex()  { return _tview!=null? _tview.getSelectedIndex() : -1; }

/**
 * Sets the selected index.
 */
public void setSelectedIndex(int anIndex)  { _tview.setSelectedIndex(anIndex); }

/**
 * Sets selected index to debug.
 */
public void setDebug()
{
    /*int ind = getSelectedIndex();
    if(ind!=DEBUG_PANE_VARS && ind!=DEBUG_PANE_EXPRS)
        _appPane.setSupportTrayIndex(DEBUG_PANE_VARS);*/
}

/**
 * Returns whether SupportTray was explicitly opened ("Show Tray" button was pressed).
 */
public boolean isExplicitlyOpened()  { return _explicitlyOpened; }

/**
 * Sets whether SupportTray was explicitly opened ("Show Tray" button was pressed).
 */
public void setExplicitlyOpened(boolean aValue)  { _explicitlyOpened = aValue; }

/**
 * Creates UI for SupportTray.
 */
protected View createUI()
{
    // Set TabOwners
    _tabOwners = new ViewOwner[] { };//_appPane.getProblemsPane(), _appPane.getRunConsole(), _appPane.getDebugVarsPane(),
        //_appPane.getDebugExprsPane(), _appPane.getBreakpointsPanel(), _appPane.getSearchPane() };

    // Create TabView, configure and return    
    _tview = new TabView(); _tview.setName("TabView"); _tview.setFont(_tview.getFont().deriveFont(12));
    _tview.setTabMinWidth(70);
    //_tpane.addTab("Problems", _appPane.getProblemsPane().getUI());
    _tview.addTab("Console", new Label("RunConsole"));
    _tview.addTab("Variables", new Label("DebugVarsPane"));
    _tview.addTab("Expressions", new Label("DebugExprsPane"));
    return _tview;
}

/**
 * Initialize UI.
 */
protected void initUI()
{
    getUI().setPrefHeight(200);
}

/**
 * Override to reset selected tab.
 */
protected void resetUI()
{
    int index = _tview.getSelectedIndex();
    //ViewOwner sowner = _tabOwners[index];
    //if(sowner!=null)
    //    sowner.resetLater();
}

/**
 * Respond to UI changes.
 */
protected void respondUI(ViewEvent anEvent)
{
    // Handle TabView
    /*if(_tpane.getTabContent(_tpane.getSelectedIndex()) instanceof Label) {
        int index = _tpane.getSelectedIndex();
        ViewOwner sowner = _tabOwners[index];
        _tpane.setTabContent(sowner.getUI(), index);
    }*/
}

}