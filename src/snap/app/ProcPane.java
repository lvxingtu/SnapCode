package snap.app;
import java.util.*;
import snap.debug.*;
import snap.gfx.*;
import snap.javatext.JavaPage;
import snap.util.*;
import snap.view.*;
import snap.viewx.WebPage;
import snap.web.*;

/**
 * The ProcPane manages run/debug processes for AppPane.
 */
public class ProcPane extends ViewOwner implements RunApp.AppListener {
    
    // The AppPane
    AppPane                _appPane;

    // The list of recently run apps
    List <RunApp>          _apps = new ArrayList();
    
    // The selected app
    RunApp                 _selApp;
    
    // The Process TreeView
    TreeView               _procTree;
    
    // Whether Console needs to be reset
    boolean                _resetConsole;
    
    // The file that currently has the ProgramCounter
    WebFile                _progCounterFile;
    
    // The current ProgramCounter line
    int                    _progCounterLine;
    
    // Images
    public static Image ProcImage = Image.get(ProcPane.class, "Process.png");
    public static Image ThreadImage = Image.get(ProcPane.class, "Thread.png");
    public static Image StackFrameImage = Image.get(ProcPane.class, "StackFrame.png");

/**
 * Creates a new ProcPane.
 */
public ProcPane(AppPane anAP)  { _appPane = anAP; }

/**
 * Returns the AppPane.
 */
public AppPane getAppPane()  { return _appPane; }

/**
 * Returns the RunConsole.
 */
public RunConsole getRunConsole()  { return _appPane.getRunConsole(); }

/**
 * Returns the DebugVarsPane.
 */
public DebugVarsPane getDebugVarsPane()  { return _appPane.getDebugVarsPane(); }

/**
 * Returns the DebugExprsPane.
 */
public DebugExprsPane getDebugExprsPane()  { return _appPane.getDebugExprsPane(); }

/**
 * Returns the list of processes.
 */
public List <RunApp> getProcs()  { return _apps; }

/**
 * Adds a new process.
 */
public void addProc(RunApp aProc)
{
    for(RunApp p : _apps.toArray(new RunApp[0])) if(p.isTerminated()) removeProc(p);
    _apps.add(aProc);
    aProc.setListener(this);
    resetLater();
}

/**
 * Removes a process.
 */
public RunApp removeProc(int anIndex)
{
    RunApp proc = _apps.remove(anIndex);
    return proc;
}

/**
 * Removes a process.
 */
public int removeProc(RunApp aProcess)
{
    int index = ListUtils.indexOfId(_apps, aProcess);
    if(index>=0) removeProc(index);
    return index;
}

/**
 * Returns the selected app.
 */
public RunApp getSelApp()  { return _selApp; }

/**
 * Sets the selected app.
 */
public void setSelApp(RunApp aProc)
{
    if(aProc==_selApp) return;
    _selApp = aProc;
    resetLater(); _resetConsole = true;
}

/**
 * Returns the debug process.
 */
public DebugApp getSelDebugApp()  { return _selApp instanceof DebugApp? (DebugApp)_selApp : null; }

/**
 * Sets the selected stack frame.
 */
public DebugFrame getSelFrame()  { DebugApp dp = getSelDebugApp(); return dp!=null? dp.getFrame() : null; }

/**
 * Returns whether selected process is running.
 */
public boolean isRunning()  { RunApp app = getSelApp(); return app!=null && app.isRunning(); }

/**
 * Returns whether selected process is terminated.
 */
public boolean isTerminated()  { RunApp app = getSelApp(); return app==null || app.isTerminated(); }

/**
 * Returns whether selected process can be paused.
 */
public boolean isPausable()  { DebugApp app = getSelDebugApp(); return app!=null && app.isRunning(); }

/**
 * Returns whether selected process is paused.
 */
public boolean isPaused()  { DebugApp app = getSelDebugApp(); return app!=null && app.isPaused(); }

/**
 * RunProc.Listener method - called when process starts.
 */
public void appStarted(RunApp aProc)
{
    resetLater(); updateProcTreeLater();
}

/**
 * RunProc.Listener method - called when process is paused.
 */
public void appPaused(DebugApp aProc)
{
    resetLater();
    updateProcTreeLater();
}

/**
 * RunProc.Listener method - called when process is continued.
 */
public void appResumed(DebugApp aProc)
{
    resetLater();
    updateProcTreeLater();
    setProgramCounter(null, -1);
}

/**
 * RunProc.Listener method - called when process ends.
 */
public void appExited(RunApp aProc)
{
    if(!isEventThread()) { runLater(() -> appExited(aProc)); return; }
    
    _procTree.updateItems(aProc);
    resetLater();
    if(_appPane.getSupportTrayIndex()==SupportTray.RUN_PANE && !aProc.hadError())
        _appPane.setSupportTrayVisible(false);
    _appPane.resetLater();
    getDebugVarsPane().resetVarTable();
    getDebugExprsPane().resetVarTable();
    setProgramCounter(null, 1);
}

/**
 * Called when DebugApp gets notice of things like VM start/death, thread start/death, breakpoints, etc.
 */
public void processDebugEvent(DebugApp aProc, DebugEvent anEvent)
{
    switch(anEvent.getType()) {
    
        // Handle ThreadStart
        case ThreadStart: updateProcTreeLater(); break;
        
        // Handle ThreadDeatch
        case ThreadDeath: updateProcTreeLater(); break;
        
        // Handle LocationTrigger
        case LocationTrigger: {
            runLater(() -> handleLocationTrigger());
            break;
        }
    }
}

/**
 * Called when Debug LocationTrigger is encountered.
 */
protected void handleLocationTrigger()
{
    getEnv().activateApp(getUI());
    getDebugVarsPane().resetVarTable();
    getDebugExprsPane().resetVarTable();
}

/**
 * RunProc.Listener method - called when stack frame changes.
 */
public void frameChanged(DebugApp aProc)
{
    // Make DebugVarsPane visible and updateVarTable
    getAppPane().getSupportTray().setDebug();
    
    DebugFrame frame = aProc.getFrame(); if(frame==null) return;
    getDebugVarsPane().resetVarTable(); // This used to be before short-circuit to clear trees
    getDebugExprsPane().resetVarTable();
    String path = frame.getSourcePath(); if(path==null) return;
    int lineNum = frame.getLineNumber(); if(lineNum<0) lineNum = 0;
    path = getRunConsole().getSourceURL(path); path += "#SelLine=" + lineNum;
    
    // Set ProgramCounter file and line
    WebURL url = WebURL.getURL(path);
    setProgramCounter(url.getFile(), lineNum-1);
    _appPane.getBrowser().setURL(url);
}

/** DebugListener breakpoint methods. */
public void requestSet(BreakpointReq e)  { }
public void requestDeferred(BreakpointReq e)  { }
public void requestDeleted(BreakpointReq e)  { }
public void requestError(BreakpointReq e)  { }

/**
 * RunProc.Listener method - called when output is available.
 */
public void appendOut(final RunApp aProc, final String aStr)
{
    if(getSelApp()==aProc)
        getRunConsole().appendOut(aStr);
}

/**
 * RunProc.Listener method - called when error output is available.
 */
public void appendErr(final RunApp aProc, final String aStr)
{
    if(getSelApp()==aProc)
        getRunConsole().appendErr(aStr);
}

/**
 * Returns the program counter for given file.
 */
public int getProgramCounter(WebFile aFile)  { return aFile==_progCounterFile? _progCounterLine : -1; }

/**
 * Sets the program counter file, line.
 */
public void setProgramCounter(WebFile aFile, int aLine)
{
    // Store old value, set new value
    WebFile oldPCF = _progCounterFile; _progCounterFile = aFile; _progCounterLine = aLine;
    
    // Reset JavaPage.TextView for old/new files
    WebPage page = oldPCF!=null? getAppPane().getBrowser().getPage(oldPCF.getURL()) : null;
    if(page instanceof JavaPage)
        ((JavaPage)page).getTextView().repaint();
    page = _progCounterFile!=null? getAppPane().getBrowser().getPage(_progCounterFile.getURL()) : null;
    if(page instanceof JavaPage)
        ((JavaPage)page).getTextView().repaint();    
}

/**
 * Initialize UI.
 */
protected void initUI()
{
    _procTree = getView("ProcTree", TreeView.class);
    _procTree.setResolver(new ProcTreeResolver());
    _procTree.setRowHeight(20);
}

/**
 * ResetUI controls.
 */
protected void resetUI()
{
    // Reset items, auto expand threads
    List <RunApp> apps = getProcs();
    _procTree.setItems(apps);
    for(RunApp app : apps) _procTree.expandItem(app);
    if(apps.size()>0) _procTree.updateItems();
    
    // If current proc is Debug with suspended thread, select current frame
    RunApp proc = getSelApp();
    DebugFrame frame = getSelFrame();
    if(frame!=null) {
        _procTree.expandItem(frame.getThread());
        _procTree.setSelectedItem(frame);
    }
    
    // Reset Console
    if(_resetConsole) { _resetConsole = false;
        getRunConsole().clear();
        if(proc!=null) for(RunApp.Output out : proc.getOutput())
            if(out.isErr()) appendErr(proc, out.getString());
            else appendOut(proc, out.getString());
    }
}

/**
 * Respond to UI controls.
 */
protected void respondUI(ViewEvent anEvent)
{
    // The Selected App, DebugApp
    RunApp app = getSelApp(); DebugApp dapp = getSelDebugApp();

    // Handle DebugButton
    if(anEvent.equals("DebugButton"))
        getAppPane().getFilesPane().debug();

    // Handle ResumeButton
    else if(anEvent.equals("ResumeButton"))
        dapp.resume();
    
    // Handle SuspendButton
    else if(anEvent.equals("SuspendButton"))
        dapp.pause();
    
    // Handle TerminateButton
    else if(anEvent.equals("TerminateButton"))
        app.terminate();

    // Handle StepIntoButton
    else if(anEvent.equals("StepIntoButton"))
        dapp.stepIntoLine();

    // Handle StepOverButton
    else if(anEvent.equals("StepOverButton"))
        dapp.stepOverLine();

    // Handle StepReturnButton
    else if(anEvent.equals("StepReturnButton"))
        dapp.stepOut();
    
    // Handle ProcTree
    else if(anEvent.equals("ProcTree")) {
        Object item = anEvent.getSelectedItem();
        if(item instanceof RunApp)
            setSelApp((RunApp)item);
        else if(item instanceof DebugFrame) { DebugFrame frame = (DebugFrame)item;
            frame.select();
            getAppPane().getSupportTray().setDebug();
        }
    }
}

/**
 * A TreeResolver for ProcTree.
 */
private class ProcTreeResolver extends TreeResolver {
    
    /** Whether given object is a parent (has children). */
    public boolean isParent(Object anItem)
    {
        if(anItem instanceof DebugApp) return !((DebugApp)anItem).isTerminated();
        if(anItem instanceof DebugThread) { DebugThread dthread = (DebugThread)anItem; DebugApp dapp = dthread.getApp();
            return dapp.isPaused() && dthread.isSuspended(); }
        return false;
    }

    /** Returns the children. */
    public Object[] getChildren(Object aParent)
    {
        if(aParent instanceof DebugApp) return ((DebugApp)aParent).getThreads();
        if(aParent instanceof DebugThread)  return ((DebugThread)aParent).getFrames();
        throw new RuntimeException("ProcPane.ProcTreeResolver: Invalid parent: " + aParent);
    }
    
    /** Returns the parent of an item. */
    public Object getParent(Object anItem)
    {
        Object par = null;
        if(anItem instanceof DebugThread) par = ((DebugThread)anItem).getApp();
        if(anItem instanceof DebugFrame) par = ((DebugFrame)anItem).getThread();
        return par;
    }

    /** Returns the text to be used for given item. */
    public String getText(Object anItem)
    {
        if(anItem instanceof RunApp) return ((RunApp)anItem).getName();
        if(anItem instanceof DebugThread) return ((DebugThread)anItem).getName();
        if(anItem instanceof DebugFrame) return ((DebugFrame)anItem).getDescription();
        return "ProcPane.ProcTreeResolver: Invalid Item: " + anItem;
    }

    /** Return the image to be used for given item. */
    public Image getImage(Object anItem)
    {
        if(anItem instanceof RunApp) return ProcImage;
        if(anItem instanceof DebugThread) return ThreadImage;
        return StackFrameImage;
    }
}

/**
 * Update RuntimeTree later.
 */
synchronized void updateProcTreeLater()
{
    if(_procTreeUpdater!=null) return; // If already set, just return
    runLaterDelayed(250, _procTreeUpdater = _procTreeUpdaterImpl);
} Runnable _procTreeUpdater, _procTreeUpdaterImpl = () -> { resetLater(); _procTreeUpdater = null; };

}