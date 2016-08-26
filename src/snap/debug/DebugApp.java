package snap.debug;
import java.io.*;
import java.util.*;
import snap.debug.Exceptions.*;
import snap.project.Breakpoint;
import snap.web.WebURL;
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

/**
 * This class manages a DebugApp.
 * Originated from pieces of debug.gui.ContentManager/Environment/CommandInterpreter and debug.bdi.ExecutionManager.
 */
public class DebugApp extends RunApp {

    // The virtual machine
    VirtualMachine                _vm;
    
    // Current thread
    ThreadReference               _currentThread;
    
    // Current frame index
    int                           _frameIndex;
    
    // Whether app has been paused
    boolean                       _paused;

    // all specs
    List <BreakpointReq>          _eventRequestSpecs = Collections.synchronizedList(new ArrayList());

    // Event source thread
    JDIEventDispatcher            _eventDispatchThread;
    
/**
 * Creates a new DebugApp.
 */
public DebugApp(WebURL aURL, String args[])  { super(aURL, args); }

/**
 * Returns whether process is paused.
 */
public boolean isPaused()  { return _paused; }

/**
 * Start a new VM.
 */
public void exec()
{
    // Get class name (if no class name, complain and return)
    String cname = getMainClassName();
    if(cname.equals("")) { failure("No main class specifed and no current default defined."); return; }
    
    // Run process and return true - was run(suspended, vmArgs, className, args);
    try {
        endSession();

        // Get command line, create session and start it
        String vmArgs = getVmArgs(), appArgs = getAppArgs(), cmdLine = cname + " " + appArgs;
        _vm = Utils.getVM(vmArgs, cmdLine, _diagnostics); _running = true;
        startSession();
        startProcessReaders();
    }

    // Complain on VMLaunchFailureException and return false
    catch(VMLaunchFailureException e) { failure("Attempt to launch main class \"" + cname +"\" failed."); }
}

/**
 * Detach.
 */
public void terminate()
{
    try {
        ensureActiveSession();
        endSession();
    }
    catch(Exception e) { failure("Failure to detach: " + e.getMessage()); }
}

/**
 * Start session.
 */
private void startSession() throws VMLaunchFailureException
{
    // Get process
    _process = _vm.process();
    
    // Start InputWriter
    PrintWriter in = new PrintWriter(new OutputStreamWriter(_process.getOutputStream()));
    Utils.InputWriter inputWriter = new Utils.InputWriter("input writer", in, _appInput);
    inputWriter.setPriority(Thread.MAX_PRIORITY-1);
    inputWriter.start();

    _vm.setDebugTraceMode(VirtualMachine.TRACE_NONE);
    notice("Connected to VM");
    _eventDispatchThread = new JDIEventDispatcher();
    _eventDispatchThread.start();

    // We must allow the deferred breakpoints to be resolved before we continue executing the class.  We could
    // optimize if there were no deferred breakpoints outstanding for a particular class. Can we do this with JDI?
    EventRequestManager em = _vm.eventRequestManager();
    ClassPrepareRequest classPrepareRequest = em.createClassPrepareRequest();
    classPrepareRequest.setSuspendPolicy(EventRequest.SUSPEND_ALL);
    classPrepareRequest.enable();
    ClassUnloadRequest classUnloadRequest = em.createClassUnloadRequest();
    classUnloadRequest.setSuspendPolicy(EventRequest.SUSPEND_NONE);
    classUnloadRequest.enable();
    ThreadStartRequest threadStartRequest = em.createThreadStartRequest();
    threadStartRequest.setSuspendPolicy(EventRequest.SUSPEND_NONE);
    threadStartRequest.enable();
    ThreadDeathRequest threadDeathRequest = em.createThreadDeathRequest();
    threadDeathRequest.setSuspendPolicy(EventRequest.SUSPEND_NONE);
    threadDeathRequest.enable();
    ExceptionRequest exceptionRequest = em.createExceptionRequest(null, false, true);
    exceptionRequest.setSuspendPolicy(EventRequest.SUSPEND_ALL);
    exceptionRequest.enable();
    
    // Notify session start
    notifyAppStarted();
}

/**
 * End session.
 */
protected void endSession()
{
    if(!_running) return;
    
    if(_eventDispatchThread!=null) {
        _eventDispatchThread.interrupt(); _eventDispatchThread = null;
        //### The VM may already be disconnected if the debuggee did a System.exit(). Exception handler here is a
        //### kludge, Rather, there are many other places where we need to handle this exception, and initiate a
        //### detach due to an error condition, e.g., connection failure.
        try { _vm.dispose(); }
        catch(VMDisconnectedException ee) { }
        notice("Disconnected from VM");
    }
    
    // Destroy process
    if(_process!=null) { // inputWriter.quit(); outputReader.quit();  errorReader.quit();
        _process.destroy(); _process = null; }

    _running = false; _paused = false; _terminated = true; setCurrentThread(null, -1);
    notifyAppExited();
}

/**
 * Pause the app.
 */
public synchronized void pause()
{
    try {
        ensureActiveSession(); _paused = true;
        _vm.suspend();
        notifyAppPaused();
    }
    
    // Failure
    catch(Exception e) { failure("Failure to interrupt: " + e.getMessage()); _paused = false; }
}

/**
 * Resume the app.
 */
public synchronized void resume()
{
    try {
        ensureActiveSession(); _paused = false;
        setCurrentThread(_currentThread, -1);
        _vm.resume();
        notifyAppResumed();
    }
    
    // Failure  //catch(VMNotInterruptedException e) { notice("Target VM is already running."); } //### failure?
    catch(Exception e) { failure("Failure to resume: " + e.getMessage()); _paused = true; }
}

/**
 * Step into line.
 */
public void stepIntoLine()
{
    ThreadReference thread = getCurrentThread(); if(thread==null) { failure("No current thread."); return; }
    try { generalStep(thread, StepRequest.STEP_LINE, StepRequest.STEP_INTO); }
    catch(Exception e) { failure("Failure to step into line: " + e.getMessage()); }
}

/**
 * Step into instruction.
 */
public void stepIntoInstruction()
{
    ThreadReference thread = getCurrentThread(); if(thread==null) { failure("No current thread."); return; }
    try { generalStep(thread, StepRequest.STEP_MIN, StepRequest.STEP_INTO); }
    catch(Exception e) { failure("Failure to step into instruction: " + e.getMessage()); }
}

/**
 * Step out.
 */
public void stepOverLine()
{
    ThreadReference thread = getCurrentThread(); if(thread==null) { failure("No current thread."); return; }
    try { generalStep(thread, StepRequest.STEP_LINE, StepRequest.STEP_OVER); }
    catch(Exception e) { failure("Failure to step over line: " + e.getMessage()); }
}

/**
 * Step out.
 */
public void stepOverInstruction()
{
    ThreadReference thread = getCurrentThread(); if(thread==null) { failure("No current thread."); return; }
    try { generalStep(thread, StepRequest.STEP_MIN, StepRequest.STEP_OVER); }
    catch(Exception e) { failure("Failure to step over instruction: " + e.getMessage()); }
}

/**
 * Step out.
 */
public void stepOut()
{
    ThreadReference thread = getCurrentThread(); if(thread==null) { failure("No current thread."); return; }
    try { generalStep(thread, StepRequest.STEP_MIN, StepRequest.STEP_OUT); }
    catch(Exception e) { failure("Failure to step out: " + e.getMessage()); }
}

/**
 * General purpose step method.
 */
private synchronized void generalStep(ThreadReference thread, int size, int depth) throws NoSessionException 
{
    ensureActiveSession();

    // Create step request
    clearPreviousStep(thread);
    EventRequestManager reqMgr = _vm.eventRequestManager();
    StepRequest request = reqMgr.createStepRequest(thread, size, depth);
    
    // We want just the next step event and no others
    request.addCountFilter(1);
    request.enable();
    
    // Resume
    setCurrentThread(_currentThread, -1);
    _vm.resume(); _paused = false;
    notifyAppResumed();
}

/**
 * Ensures active session.
 */
void ensureActiveSession() throws NoSessionException  { if(!_running) throw new NoSessionException(); }

/*
 * Stepping.
 */
void clearPreviousStep(ThreadReference thread)
{
    // A previous step may not have completed on this thread; if so, it gets removed here. 
    EventRequestManager mgr = _vm.eventRequestManager();
    List <StepRequest> requests = mgr.stepRequests();
    for(StepRequest request : requests)
        if(request.thread().equals(thread)) {
            mgr.deleteEventRequest(request); break; }
}

/**
 * Returns the DebugThreads for a DebugApp.
 */
public DebugThread[] getThreads()
{
    // Get threads, sort (should trigger VMDisconnectedException if VM has disconnected) and return
    try {
        ThreadReference threads[] = _vm.allThreads().toArray(EMPTY_THREADREFS);
        DebugThread dthreads[] = new DebugThread[threads.length];
        for(int i=0,iMax=threads.length;i<iMax;i++) dthreads[i] = getThread(threads[i]);
        Arrays.sort(dthreads);
        return dthreads;
    }
    
    // If there is no session or VM is dead, just returns empty list
    catch(Exception e) { System.err.println("DebugApp.getThreads: " + e); return new DebugThread[0]; }
}

/** Returns a DebugThread instance for given ThreadReference. */
private DebugThread getThread(ThreadReference aTR)
{
    String name = aTR.name();
    DebugThread dt = _threads.get(name);
    if(dt==null) _threads.put(name, dt=new DebugThread(this,aTR)); else dt._tref = aTR;
    return dt;
} Map <String,DebugThread> _threads = new HashMap(); ThreadReference EMPTY_THREADREFS[] = new ThreadReference[0];

/**
 * Return a list of ThreadReference objects corresponding to the threads that are currently active in the VM.
 * A thread is removed from the list just before the thread terminates.
 */
public List <ThreadReference> allThreads()
{
    // Get threads, sort (should trigger VMDisconnectedException if VM has disconnected) and return
    try {
        List <ThreadReference> threads = new ArrayList(_vm.allThreads());
        Collections.sort(threads, new Comparator<ThreadReference>() {
            public int compare(ThreadReference o1, ThreadReference o2) { return o1.name().compareTo(o2.name()); }});
        return threads;
    }
    
    // If there is no session or VM is dead, just returns empty list
    catch(Exception e) { return Collections.emptyList(); }
}

/** Return a list of ThreadGroupReference objects corresponding to top-level threadgroups that are currently active. */
//public List topLevelThreadGroups() throws NoSessionException{ensureActiveSession();return _vm.topLevelThreadGroups();}
/** Return the system threadgroup. */
//public ThreadGroupReference systemThreadGroup() throws NoSessionException
//{ ensureActiveSession(); return _vm.topLevelThreadGroups().get(0);}

/**
 * Thread control.
 */
void pauseThread(ThreadReference thread) throws NoSessionException  { ensureActiveSession(); thread.suspend(); }
void resumeThread(ThreadReference thread) throws NoSessionException  { ensureActiveSession(); thread.resume(); }
void stopThread(ThreadReference thread) throws NoSessionException  { ensureActiveSession(); } //thread.stop();

/**
 * Returns the current thread.
 */
public DebugThread getThread()  { return _currentThread!=null? getThread(_currentThread) : null; }

/**
 * Returns the current frame.
 */
public DebugFrame getFrame()
{
    int ind = _frameIndex; //getCurrentFrameIndex();
    StackFrame frame = ind>=0? getCurrentFrame() : null;
    DebugThread thread = frame!=null? getThread() : null;
    return thread!=null? new DebugFrame(thread,frame,ind) : null;
}

/**
 * Sets the current frame.
 */
public void setFrame(DebugFrame aFrame)
{
    ThreadReference tref = aFrame!=null? aFrame._thread._tref : null;
    int ind = aFrame!=null? aFrame.getIndex() : -1;
    setCurrentThread(tref,ind);
}

/**
 * Returns the current thread.
 */
public ThreadReference getCurrentThread()  { return _currentThread; }

/**
 * Sets the current thread.
 */
public void setCurrentThread(ThreadReference aThread, int aFrameIndex)
{
    if(aThread==_currentThread && aFrameIndex==_frameIndex) return;
    _currentThread = aThread; _frameIndex = aFrameIndex;
    notifyFrameChanged();
}

/**
 * Returns the current stack frame.
 */
public StackFrame getCurrentFrame()
{
    try { return _running && _currentThread!=null && _frameIndex>=0? _currentThread.frame(_frameIndex) : null; }
    catch(Exception e)  { System.err.println("DebugApp.getCurrentThread: " + e); return null; }
}

/**
 * Returns the current stack frame index.
 */
public int getCurrentFrameIndex()  { return _running && _paused? _frameIndex : -1; }

/**
 * Send line of input to app.
 */
public void sendLineToApp(String line)
{
    synchronized(inputLock) {
        inputBuffer.addFirst(line);
        inputLock.notifyAll();
    }
}

/** SendLineToApp support. */
private LinkedList inputBuffer = new LinkedList(); private Object inputLock = new Object();
private InputListener _appInput = new InputListener() {
    public String getLine()
    {
        // Don't allow reader to be interrupted -- catch and retry.
        String line = null; while(line==null) {
            synchronized (inputLock) {
                try {
                    while(inputBuffer.size() < 1) inputLock.wait();
                    line = (String)inputBuffer.removeLast();
                } catch (InterruptedException e) { }
            }
        }

        // Must not be holding inputLock here, as listener that we call to echo line might call us re-entrantly
        echoInputLine(line);
        return line;
    }
};

/**
 * Called to echo input.
 */
private void echoInputLine(String line)  { }

/**
 * Returns the list of user defined EventRequests.
 */
public List <BreakpointReq> getEventRequestSpecs()
{
    // We need to make a copy to avoid synchronization problems
    synchronized(_eventRequestSpecs) { return new ArrayList(_eventRequestSpecs); }
}

/**
 * Adds a breakpoint.
 */
public void addBreakpoint(Breakpoint aBP)
{
    // If Breakpoint already set, just return
    for(BreakpointReq bpr : _eventRequestSpecs)
        if(bpr._bpoint.equals(aBP)) { System.err.println("Breakpoint already added " + aBP); return; }

    // Create new BreakpointReq and add
    BreakpointReq bpr = new BreakpointReq(this, aBP);
    synchronized(_eventRequestSpecs) { _eventRequestSpecs.add(bpr); }  // Add request to list
    if(_vm!=null && !_terminated)  // Have event resolve immediately
        bpr.install(_vm);
}

/**
 * Removes a breakpoint.
 */
public void removeBreakpoint(Breakpoint aBP)
{
    // Find BreakpointReq
    BreakpointReq bpr = null; for(BreakpointReq br : _eventRequestSpecs) if(br._bpoint==aBP) { bpr = br; break; }
    if(bpr==null) return;
    
    // Remove BreakpointReq
    synchronized (_eventRequestSpecs) { _eventRequestSpecs.remove(bpr); }  // Remove event from list
    bpr.delete();  // Delete
}

/** 
 * Resolve all deferred eventRequests waiting for 'refType'.
 */
protected void resolve(ReferenceType refType)
{
    synchronized(_eventRequestSpecs) {
        for(BreakpointReq ers : _eventRequestSpecs)
            ers.attemptResolve(refType); }
}

/**
 * Return a list of ReferenceType objects for all currently loaded classes and interfaces. Array types are not returned.
 */
public List <ReferenceType> allClasses() throws NoSessionException
{
    ensureActiveSession();
    return _vm.allClasses();
}

/**
 * Return a ReferenceType object for the currently loaded class or interface whose fully-qualified
 * class name is specified, else return null if there is none. In general, we must return a list of types, because
 * multiple class loaders could have loaded a class with the same fully-qualified name.
 */
public List findClassesByName(String name) throws NoSessionException
{
    ensureActiveSession();
    return _vm.classesByName(name);
}

/**
 * Return a list of ReferenceType objects for all currently loaded classes and interfaces whose name matches the given
 * pattern.  The pattern syntax is open to some future revision, but currently consists of a fully-qualified class name
 * in which the first component may optionally be a "*" character, designating an arbitrary prefix.
 */
public List findClassesMatchingPattern(String aPattern) throws NoSessionException
{
    ensureActiveSession();

    // Wildcard matches any leading package name.
    if(aPattern.startsWith("*.")) {
        String pattern = aPattern.substring(1);
        List result = new ArrayList();  //### Is default size OK?
        List <ReferenceType> classes = _vm.allClasses();
        for(ReferenceType type : classes)
            if (type.name().endsWith(pattern))
                result.add(type);
        return result;
    }

    // It's a class name.
    return _vm.classesByName(aPattern);
}

/**
 * Notifications for app start, exit, interrupted, continued.
 */
protected void notifyAppStarted()  { _listener.appStarted(this); }
protected void notifyAppPaused()  { _listener.appPaused(this); }
protected void notifyAppResumed()  { _listener.appResumed(this); }
protected void notifyAppExited()  { _listener.appExited(this); }

/**
 * Notification that current StackFrame changed.
 * This notification is fired only in response to USER-INITIATED changes to the current thread and current frame.
 * When the current thread is set automatically after a breakpoint hit or step completion, no event is generated.
 * Instead, interested parties are expected to listen for the BreakpointHit and StepCompleted events.  This convention
 * is unclean, and I believe that it reflects a defect in the current architecture.  Unfortunately, however, we cannot
 * guarantee the order in which various listeners receive a given event, and the handlers for the very same events that
 * cause automatic changes to the current thread may also need to know the current thread.
 */
protected void notifyFrameChanged()  { _listener.frameChanged(this); }

/**
 * Notifications for breakpoints.
 */
protected void notifySet(BreakpointReq aBP)  { _listener.requestSet(aBP); }
protected void notifyDeferred(BreakpointReq aBP)  { _listener.requestDeferred(aBP); }
protected void notifyDeleted(BreakpointReq aBP)  { _listener.requestDeleted(aBP); }
protected void notifyError(BreakpointReq aBP)  { error("Failed to set BP: " + aBP); _listener.requestError(aBP); }

/**
 * JDI EventSet notifications (runs in Application EventDispatch Thread).
 */
synchronized void notifyJDIEvent(JDIEventSet anES)
{
    // Notify listeners
    boolean interrupted = anES.suspendedAll(), wantsInterrupt = false;

    // Handle event types (VMStart, VMDeath, VMDisconnect, ThreadStart, ThreadDeath, ClassPrepare, ClassUnload)
    switch(anES._type) {
    
        // Handle VMDisconnect
        case VMDisconnect: {
            endSession(); break; }
        
        // Handle LocationTrigger
        case LocationTrigger: { wantsInterrupt = true;
            setCurrentThread(anES.getThread(), 0); break; }
        
        // Handle Exception
        case Exception: { wantsInterrupt = true;
            setCurrentThread(anES.getThread(), 0); break; }
        
        // Handle AccessWatchpoint
        case AccessWatchpoint: wantsInterrupt = true; break;
        
        // Handle ModificationWatchpoint
        case ModificationWatchpoint: wantsInterrupt = true; break;
    }
    
    // Restart VM (unless stopping was part of event)
    if(interrupted && !wantsInterrupt)
        try { _vm.resume(); _paused = false; }
        catch(VMDisconnectedException ee) { }
    
    // Otherwise, make interruption official
    else if(interrupted && !_paused) { _paused = true;
        notifyAppPaused(); }
    
    // Dispatch event to listener
    if(_listener!=null) _listener.processJDIEvent(this, anES);
}

/**
 * A thread to read and send events from vm.
 */
public class JDIEventDispatcher extends Thread {

    EventQueue         _queue;

    /** Create JDIEventDispatcher. */
    public JDIEventDispatcher()  { super("JDI Event Set Dispatcher"); _queue = _vm.eventQueue(); }
    
    /** Override for thread meat. */
    public void run()
    {
        try { runLoop(); }
        catch(Exception exc) { } // Do something different for InterruptedException??? // just exit
        _running = false; _paused = false; _terminated = true;
    }
    
    /** Run Loop. */
    private void runLoop() throws InterruptedException
    {
        while(true) {
            
            // Dequeue event and create JDIEventSet
            EventSet jdiEventSet = _queue.remove();
            final JDIEventSet eventSet = new JDIEventSet(jdiEventSet); //_interrupted = eventSet.suspendedAll();
            
            // Handle Class prepare
            if(eventSet._type==JDIEventSet.Type.ClassPrepare) {
                resolve(eventSet.getReferenceType());
                try { _vm.resume(); _paused = false; }
                catch(VMDisconnectedException ee) { }
            }
            
            // Dispatch event - in JavaFX thread
            else snap.view.ViewEnv.getEnv().runLater(() -> notifyJDIEvent(eventSet));
            
            // Quit on VMDisconnect
            if(eventSet.getType()==JDIEventSet.Type.VMDisconnect) break;
        }
    }
}

}