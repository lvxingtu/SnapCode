package snap.playground;
import snap.view.*;

/**
 * A class to hold TabView for ProblemsPane, RunConsole, DebugPane.
 */
public class PGTabPane extends ViewOwner {

    // The Playground
    Playground     _pg;
    
    // The tabview
    TabView        _tview;
    
    // The list of tab owners
    ViewOwner      _tabOwners[];
    
    // Constants for tabs
    public static final int CONSOLE_PANE = 0;
    public static final int RUN_PANE = 1;
    public static final int DEBUG_PANE_VARS = 2;
    public static final int BREAKPOINTS_PANE = 4;
    
/**
 * Creates a new PGTabPane for given Playground.
 */
public PGTabPane(Playground aPG)  { _pg = aPG; }

/**
 * Returns the selected index.
 */
public int getSelectedIndex()  { return _tview!=null? _tview.getSelIndex() : -1; }

/**
 * Sets the selected index.
 */
public void setSelectedIndex(int anIndex)  { _tview.setSelIndex(anIndex); }

/**
 * Creates UI for SupportTray.
 */
protected View createUI()
{
    // Set TabOwners
    PGConsole console = _pg.getConsole();
    _tabOwners = new ViewOwner[] { console }; //, _appPane.getRunConsole(), _appPane.getDebugVarsPane(),
        //_appPane.getDebugExprsPane(), _appPane.getBreakpointsPanel(), _appPane.getSearchPane() };

    // Create TabView, configure and return    
    _tview = new TabView(); _tview.setName("TabView"); _tview.setFont(_tview.getFont().deriveFont(12));
    _tview.setTabMinWidth(70);
    //_tpane.addTab("Problems", _appPane.getProblemsPane().getUI());
    _tview.addTab("Console", console.getUI());
    _tview.addTab("Variables", new Label("DebugVarsPane"));
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
    int index = _tview.getSelIndex();
    ViewOwner sowner = _tabOwners[index];
    if(sowner!=null)
        sowner.resetLater();
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