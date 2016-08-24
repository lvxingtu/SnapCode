package snap.debug;
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;
import java.util.*;

public class JDIEventSet extends EventObject { //implements EventSet
    
    // The original event set
    EventSet            jdiEventSet;
    
    // The first event from event set
    Event               oneEvent;
    
    // The type
    Type                _type;
    
    // Constants for type
    public enum Type {
        Exception, AccessWatchpoint, ModificationWatchpoint, LocationTrigger, ClassPrepare,
        ClassUnload, ThreadDeath, ThreadStart, VMDeath, VMDisconnect, VMStart
    }

/**
 * Create new JDIEventSet.
 */
JDIEventSet(EventSet jdiEventSet)
{
    // Do normal version
    super(jdiEventSet.virtualMachine());
    
    // Set event set and get first event
    this.jdiEventSet = jdiEventSet;
    this.oneEvent = jdiEventSet.eventIterator().nextEvent();
    
    // Set type
    if(oneEvent instanceof VMStartEvent) _type = Type.VMStart;
    else if(oneEvent instanceof VMDeathEvent) _type = Type.VMDeath;
    else if(oneEvent instanceof VMDisconnectEvent) _type = Type.VMDisconnect;
    else if(oneEvent instanceof ThreadStartEvent) _type = Type.ThreadStart;
    else if(oneEvent instanceof ThreadDeathEvent) _type = Type.ThreadDeath;
    else if(oneEvent instanceof ExceptionEvent) _type = Type.Exception;
    else if(oneEvent instanceof AccessWatchpointEvent) _type = Type.AccessWatchpoint;
    else if(oneEvent instanceof WatchpointEvent) _type = Type.ModificationWatchpoint;
    else if(oneEvent instanceof ClassPrepareEvent) _type = Type.ClassPrepare;
    else if(oneEvent instanceof ClassUnloadEvent) _type = Type.ClassUnload;
    else if(oneEvent instanceof LocatableEvent) _type = Type.LocationTrigger;
    else throw new IllegalArgumentException("Unknown event " + oneEvent);
    
    //System.out.println("JDIEvent: " + _type);
}

/**
 * Returns the type.
 */
public Type getType()  { return _type; }

/**
 * Gets the thrown exception object. The exception object is an instance of Throwable or a subclass in the target VM.
 * @return an {@link ObjectReference} which mirrors the thrown object in the target VM. 
 */
public ObjectReference getException()  { return ((ExceptionEvent)oneEvent).exception(); }

/**
 * Gets the location where the exception will be caught. An exception is considered to be caught if, at the point
 * of the throw, the current location is dynamically enclosed in a try statement that handles the exception.
 * (See the JVM specification for details). If there is such a try statement, the catch location is the 
 * first code index of the appropriate catch clause. <p>
 * If there are native methods in the call stack at the time of the exception, there are important restrictions
 * to note about the returned catch location. In such cases, it is not possible to predict whether an exception
 * will be handled by some native method on the call stack. Thus, it is possible that exceptions considered
 * uncaught here will, in fact, be handled by a native method and not cause termination of the target VM. Also,
 * it cannot be assumed that the catch location returned here will ever be reached by the throwing thread. If
 * there is a native frame between the current location and the catch location, the exception might be handled
 * and cleared in that native method instead.
 * @return the {@link Location} where the exception will be caught or null if the exception is uncaught.
 */
public Location getCatchLocation()  { return ((ExceptionEvent)oneEvent).catchLocation(); }

/**
 * Value that will be assigned to the field when the instruction completes.
 */
public Value getValueToBe()  { return ((ModificationWatchpointEvent)oneEvent).valueToBe(); }

/**
 * Returns the thread in which this event has occurred. 
 * @return a {@link ThreadReference} which mirrors the event's thread in the target VM.
 */
public ThreadReference getThread()
{
    if(oneEvent instanceof LocatableEvent) return ((LocatableEvent)oneEvent).thread();
    if(oneEvent instanceof ClassPrepareEvent) return ((ClassPrepareEvent)oneEvent).thread();
    if(oneEvent instanceof ThreadDeathEvent) return ((ThreadDeathEvent)oneEvent).thread();
    if(oneEvent instanceof ThreadStartEvent) return ((ThreadStartEvent)oneEvent).thread();
    if(oneEvent instanceof VMStartEvent) return ((VMStartEvent)oneEvent).thread();
    throw new RuntimeException("JDIEventSet.getThread: Can't return thread for " + _type);
}

/**
 * Returns the {@link Location} of this mirror. Depending on context and on available debug information, this
 * location will have varying precision.
 * @return the {@link Location} of this mirror.
 */
public Location getLocation()  { return ((LocatableEvent)oneEvent).location(); }

/**
 * Returns the reference type for which this event was generated.
 * @return a {@link ReferenceType} which mirrors the class, interface, or array which has been linked.
 */
public ReferenceType getReferenceType()  { return ((ClassPrepareEvent)oneEvent).referenceType(); }

/**
 * Returns the name of the class that has been unloaded.
 */
public String getClassName()  { return ((ClassUnloadEvent)oneEvent).className(); }

/**
 * Returns the JNI-style signature of the class that has been unloaded.
 */
public String getClassSignature() { return ((ClassUnloadEvent)oneEvent).classSignature(); }

/**
 * Returns the field that is about to be accessed/modified. 
 * @return a {@link Field} which mirrors the field in the target VM.
 */
public Field getField()  { return ((WatchpointEvent)oneEvent).field(); }

/**
 * Returns the object whose field is about to be accessed/modified. 
 * Return null is the access is to a static field.
 * @return a {@link ObjectReference} which mirrors the event's object in the target VM.
 */
public ObjectReference getObject()  { return ((WatchpointEvent)oneEvent).object(); }

/**
 * Current value of the field.
 */
public Value getValueCurrent()  { return ((WatchpointEvent)oneEvent).valueCurrent(); }

// Implement Mirror
//public VirtualMachine virtualMachine()  { return jdiEventSet.virtualMachine(); }
//public VirtualMachine getVirtualMachine()  { return jdiEventSet.virtualMachine(); }

/**
 * Returns the policy used to suspend threads in the target VM for this event set. This policy is selected from
 * the suspend policies for each event's request. The one that suspends the most threads is chosen when the event
 * occurs in the target VM and that policy is returned here.
 * See com.sun.jdi.request.EventRequest for the possible policy values.
 * 
 * @return the integer suspendPolicy
 */
//public int getSuspendPolicy() { return jdiEventSet.suspendPolicy(); }
//public void resume() { jdiEventSet.resume(); }
//public int suspendPolicy() { return jdiEventSet.suspendPolicy(); }
public boolean suspendedAll() { return jdiEventSet.suspendPolicy() == EventRequest.SUSPEND_ALL; }
//public boolean suspendedEventThread() { return jdiEventSet.suspendPolicy() == EventRequest.SUSPEND_EVENT_THREAD; }
//public boolean suspendedNone() { return jdiEventSet.suspendPolicy() == EventRequest.SUSPEND_NONE; }

/** Return an iterator specific to {@link Event} objects. */
//public EventIterator eventIterator() { return jdiEventSet.eventIterator(); }

// Implement java.util.Set (by pass through)
//public int size() { return jdiEventSet.size(); }
//public boolean isEmpty() { return jdiEventSet.isEmpty(); }
//public boolean contains(Object o) { return jdiEventSet.contains(o); }
//public Iterator<Event> iterator() { return jdiEventSet.iterator(); }
//public Object[] toArray() { return jdiEventSet.toArray(); }
//public <T> T[] toArray(T a[]) { return jdiEventSet.toArray(a); }
//public boolean containsAll(Collection<?> c) { return jdiEventSet.containsAll(c); }
//public boolean add(Event e)  { throw new UnsupportedOperationException(); }
//public boolean remove(Object o)  { throw new UnsupportedOperationException(); }
//public boolean addAll(Collection<? extends Event> coll)  { throw new UnsupportedOperationException(); }
//public boolean removeAll(Collection<?> coll)  { throw new UnsupportedOperationException(); }
//public boolean retainAll(Collection<?> coll)  { throw new UnsupportedOperationException(); }
//public void clear()  { throw new UnsupportedOperationException(); }

/**
 * Standard toString implementation.
 */
public String toString()  { return "JDIEventSet: " + _type; }

}