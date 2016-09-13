/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snap.javatext;
import java.util.*;
import snap.gfx.*;
import snap.javaparse.*;
import snap.project.*;
import snap.view.*;

/**
 * A component to paint row markers.
 */
public class RowHeader extends View {

    // The JavaTextView
    JavaTextView             _textView;

    // The list of markers
    List <Marker>            _markers;
    
    // The last mouse moved position
    double                   _mx, _my;
    
    // Width of this component
    public static final int  WIDTH = 12;
    
    // The marker images for Error, Warning, Breakpoint, Implements, Override
    static Image  _errorImage = Image.get(JavaTextBox.class, "ErrorMarker.png");
    static Image  _warningImage = Image.get(JavaTextBox.class, "WarningMarker.png");
    static Image  _breakpointImage = Image.get(JavaTextBox.class, "Breakpoint.png");
    static Image  _implImage = Image.get(JavaTextBox.class, "ImplementsMarker.png");
    static Image  _overImage = Image.get(JavaTextBox.class, "pkg.images/OverrideMarker.png");

/**
 * Creates a new RowHeader.
 */
public RowHeader()
{
    enableEvents(MouseMoved, MouseClicked);
    setToolTipEnabled(true);
    setFill(new Color(233,233,233));
    setPrefWidth(WIDTH);
}

/**
 * Returns the JavaTextView.
 */
public JavaTextView getTextView()  { return _textView; }

/**
 * Sets the JavaTextView.
 */
public void setTextView(JavaTextView aJTA)  { _textView = aJTA; }

/**
 * Sets the JavaTextView selection.
 */
public void setTextSelection(int aStart, int anEnd)  { _textView.setSel(aStart, anEnd); }

/**
 * Returns the list of markers.
 */
public List <Marker> getMarkers() { return _markers!=null? _markers : (_markers=createMarkers()); }

/**
 * Returns the list of markers.
 */
protected List <Marker> createMarkers()
{
    // Create list
    List <Marker> markers = new ArrayList();
    
    // Add markers for member Overrides/Implements
    JClassDecl cd = _textView.getJFile().getClassDecl();
    if(cd!=null)
        getSuperMemberMarkers(cd, markers);

    // Add markers for BuildIssues
    BuildIssue buildIssues[] = _textView.getBuildIssues();
    for(BuildIssue issue : buildIssues)
        markers.add(new BuildIssueMarker(issue));
        
    // Add markers for breakpoints
    for(Breakpoint bp : _textView.getBreakpoints())
        markers.add(new BreakpointMarker(bp));
    
    // Return markers
    return markers;
}

/**
 * Loads a list of SuperMemberMarkers for a class declaration (recursing for inner classes).
 */
private void getSuperMemberMarkers(JClassDecl aCD, List <Marker> theMarkers)
{
    for(JMemberDecl md : aCD.getMemberDecls()) {
        if(md.getSuperDecl()!=null)
            theMarkers.add(new SuperMemberMarker(md));
        if(md instanceof JClassDecl)
            getSuperMemberMarkers((JClassDecl)md, theMarkers);
    }
}

/**
 * Override to reset markers.
 */
protected void resetAll()  { _markers = null; repaint(); }

/**
 * Handle events.
 */
protected void processEvent(ViewEvent anEvent)
{
    // Handle MouseClicked
    if(anEvent.isMouseClicked()) {
        
        // Get reversed markers (so click effects top marker)
        List <Marker> markers = new ArrayList(getMarkers()); Collections.reverse(markers);
        double x = anEvent.getX(), y = anEvent.getY();
        
        // Handle double click
        if(anEvent.getClickCount()==2) {
        for(Marker marker : markers)
            if(marker.contains(x,y) && marker instanceof BreakpointMarker) {
                marker.mouseClicked(anEvent); return; }
            TextBoxLine line = _textView.getTextBox().getLineForY(anEvent.getY());
            int index = line.getIndex();
            _textView.addBreakpoint(index);
            resetAll();
            return;
        }
        
        // Handle normal click
        for(Marker marker : markers)
            if(marker.contains(x,y)) {
                marker.mouseClicked(anEvent); return; }
    }

    // Handle MouseMoved
    else if(anEvent.isMouseMoved()) {
        _mx = anEvent.getX(); _my = anEvent.getY();
        for(Marker marker : getMarkers())
            if(marker.contains(_mx,_my)) {
                setCursor(Cursor.HAND); return; }
        setCursor(Cursor.DEFAULT);
    }
}

/**
 * Paint markers.
 */
protected void paintFront(Painter aPntr)
{
    double th = _textView.getHeight(), h = Math.min(getHeight(), th);
    aPntr.setStroke(Stroke.Stroke1);
    for(Marker m : getMarkers())
        aPntr.drawImage(m._image, m.x, m.y);
}

/**
 * Override to return tool tip text.
 */
public String getToolTip(ViewEvent anEvent)
{
    for(Marker marker : getMarkers())
        if(marker.contains(_mx, _my))
            return marker.getToolTip();
    return null;
}

/**
 * The class that describes a overview marker.
 */
public abstract static class Marker<T> extends Rect {

    /** The object that is being marked. */
    T       _target;
    
    // The image
    Image   _image;
    
    /** Creates a new marker for target. */
    public Marker(T aTarget)  { _target = aTarget; setRect(-2, 0, WIDTH, WIDTH); }
    
    /** Returns a tooltip. */
    public abstract String getToolTip();
    
    /** Handles MouseClicked. */
    public abstract void mouseClicked(ViewEvent anEvent);
}

/**
 * A Marker for super members.
 */
public class SuperMemberMarker extends Marker <JMemberDecl> {

    JavaDecl _superDecl; boolean _interface;

    /** Creates a new marker for target. */
    public SuperMemberMarker(JMemberDecl aTarget)
    {
        super(aTarget);
        _superDecl = aTarget.getSuperDecl();
        _interface = aTarget.isSuperDeclInterface();
        TextBoxLine line = _textView.getLine(aTarget.getLineIndex());
        setY(Math.round(line.getY()));
        _image = isInterface()? _implImage : _overImage;
    }
    
    /** Returns whether is interface. */
    public boolean isInterface()  { return _interface; }
    
    /** Returns a tooltip. */
    public String getToolTip()
    {
        String cname = _superDecl.getClassName();
        return (isInterface()? "Implements " : "Overrides ") + cname + '.' + _target.getName();
    }
    
    /** Handles MouseClicked. */
    public void mouseClicked(ViewEvent anEvent)
    {
        JavaTextPane tp = _textView.getTextPane(); if(tp==null) return;
        tp.openSuperDeclaration(_target);
    }
}

/**
 * A Marker subclass for BuildIssues.
 */
public class BuildIssueMarker extends Marker <BuildIssue> {

    // Whether issue is error
    boolean     _isError;

    /** Creates a new marker for target. */
    public BuildIssueMarker(BuildIssue aTarget)
    {
        super(aTarget); _isError = aTarget.isError();
        TextBoxLine line = _textView.getLineAt(aTarget.getEnd());
        setY(Math.round(line.getY()));
        _image = _isError? _errorImage : _warningImage;
    }
    
    /** Returns a tooltip. */
    public String getToolTip()  { return _target.getText(); }
    
    /** Handles MouseClicked. */
    public void mouseClicked(ViewEvent anEvent)  { setTextSelection(_target.getStart(), _target.getEnd()); }
}

/**
 * A Marker subclass for Breakpoints.
 */
public class BreakpointMarker extends Marker <Breakpoint> {
    
    /** Creates a BreakpointMarker. */
    public BreakpointMarker(Breakpoint aBP)
    {
        super(aBP);
        TextBoxLine line = _textView.getLine(aBP.getLine());
        setY(Math.round(line.getY()));
        _image = _breakpointImage;
    }

    /** Returns a tooltip. */
    public String getToolTip()  { return _target.toString(); }
    
    /** Handles MouseClicked. */
    public void mouseClicked(ViewEvent anEvent)
    {
        if(anEvent.getClickCount()==2) _textView.removeBreakpoint(_target);
        resetAll();
    }
}

}