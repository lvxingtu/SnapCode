package snap.javasnap;
import java.util.*;
import snap.gfx.*;
import snap.javaparse.*;
import snap.view.*;

/**
 * A custom class.
 */
public class SnapPart <JNODE extends JNode> extends ViewOwner {

    // The JNode
    JNODE                _jnode;
    
    // The parent node owner
    SnapPart             _parent;
    
    // The children node owners
    List <SnapPart>      _children;
    
    // Whether part is selected
    boolean              _selected;
    
    // The SnapPartPane
    SnapPartPane         _pane;
    
    // The current drag over node
    static SnapPartPane  _dragOver;
    
    // Shared list of no children
    static List          _noChildren = Collections.EMPTY_LIST;
   
    // Constants for colors
    public static Color PieceColor = Color.get("#4C67d6");
    public static Color BlockStmtColor = Color.get("#8f56e3");
    public static Color MemberDeclColor = Color.get("#f0a822");
    
/**
 * Creates a SnapPart for a JNode.
 */
public static SnapPart createSnapPart(JNode aNode)
{
    SnapPart np = null;
    if(aNode instanceof JFile) np = new SnapPartFile();
    else if(aNode instanceof JMemberDecl) np = SnapPartMemberDecl.createSnapPart(aNode);
    else if(aNode instanceof JStmt) np = SnapPartStmt.createSnapPart(aNode);
    else if(aNode instanceof JExpr) np = SnapPartExpr.createSnapPart(aNode);
    if(np==null) return null;
    np.setJNode(aNode);
    return np;
}

/**
 * Returns the JNode.
 */
public JNODE getJNode()  { return _jnode; }

/**
 * Sets the JNode.
 */
public void setJNode(JNODE aJNode)  { _jnode = aJNode; }

/**
 * Returns the SnapCodeArea.
 */
public SnapEditor getCodeArea()  { return getParent()!=null? getParent().getCodeArea() : null; }

/**
 * Returns whether NodePane is a block.
 */
public boolean isBlock()  { return getJNode().isBlock(); }

/**
 * Returns the parent.
 */
public SnapPart getParent()  { return _parent; }

/**
 * Returns the ancestor of given class.
 */
public <T> T getAncestor(Class <T> aClass)
{
    for(SnapPart p=_parent;p!=null;p=p.getParent()) if(aClass.isInstance(p)) return (T)p;
    return null;
}

/**
 * Returns the number of children.
 */
public int getChildCount()  { return _children!=null? _children.size() : 0; }

/**
 * Returns the individual child.
 */
public SnapPart getChild(int anIndex)  { return _children.get(anIndex); }

/**
 * Returns the individual child.
 */
public SnapPart getChildLast()  { int cc = getChildCount(); return cc>0? _children.get(cc-1) : null; }

/**
 * Returns the children.
 */
public List <SnapPart> getChildren()
{
    if(_children==null) {
        _children = createChildren(); for(SnapPart child : _children) child._parent = this; }
    return _children;
}

/**
 * Creates the children.
 */
protected List <SnapPart> createChildren()
{
    if(!isBlock()) return _noChildren;
    
    List <SnapPart> children = new ArrayList();
    JNode node = getJNode();
    if(node.getBlock()!=null) for(JStmt stmt : node.getBlock().getStatements()) {
        SnapPart mcnp = createSnapPart(stmt); if(mcnp==null) continue;
        children.add(mcnp);
    }
    return children;
}

/**
 * Returns whether part is selected.
 */
public boolean isSelected()  { return _selected; }

/**
 * Sets whether part is selected.
 */
public void setSelected(boolean aValue)  { _selected = aValue; _pane.setSelected(aValue); }

/**
 * Returns a string describing the part.
 */
public String getPartString()  { return getJNode().getNodeString(); }

/**
 * Standard toString implementation.
 */
public String toString()  { return getJNode().toString(); }

/**
 * Returns part width.
 */
public double getWidth()  { return getUI().getWidth(); }

/**
 * Returns part height.
 */
public double getHeight()  { return getUI().getHeight(); }

/**
 * Returns the SnapPart of a node.
 */
public static SnapPart getSnapPart(View aView)
{
    if(aView==null) return null;
    ViewOwner o = aView.getOwner();
    return o instanceof SnapPart? (SnapPart)o : getSnapPart(aView.getParent());
}

/**
 * Init UI.
 */
public void initUI()
{
    if(getAncestor(SnapPartFile.class)==null) return;
    enableEvents(getUI(), DragEvents);
}

/**
 * Creates UI.
 */
protected View createUI()
{
    // Create SnapPartPane
    _pane = new SnapPartPane();
    
    // Add horizontal UI
    configureHBox(_pane.getHBox());
    if(_pane.getHBox().getChildCount()>0 && _pane.getHBox().getChild(0) instanceof TextField)
        _pane.getHBox().setPadding(0,2,0,0);
    
    // Add child UI
    if(isBlock()) {
        _pane.getVBox();
        for(SnapPart child : getChildren())
            _pane.getVBox().addChild(child.getUI());
    }
    
    // Return SnapPartPane
    return _pane;
}

/**
 * Creates a statement label.
 */
protected Label createLabel(String aString)
{
    Label label = new Label(aString); label.setPadding(3,4,4,8);
    label.setFont(new Font("Arial Bold", 11)); label.setTextFill(Color.WHITE);
    return label;
}

/**
 * Creates a statement textfield.
 */
protected TextField createTextField(String aString)
{
    TextField tfield = new TextField(); tfield.setText(aString);// tfield.setPadding(0,0,0,8);
    tfield.setFont(new Font("Arial", 11)); tfield.setAlign(Pos.CENTER); //tfield.setRadius(8);
    return tfield;
}

/**
 * Creates the UI for the top line.
 */
protected void configureHBox(HBox spane)  { }

/**
 * Respond to UI.
 */
protected void respondUI(ViewEvent anEvent)
{
    // Handle DragEvent: Accept drag event
    if(anEvent.isDragEvent())
        handleDragEvent(anEvent);
}

/**
 * Responds to drag events.
 */
protected void handleDragEvent(ViewEvent anEvent)
{
    // Handle DragEvent: Accept drag event
    //DragEvent de = anEvent.getEvent(DragEvent.class); de.acceptTransferModes(TransferMode.COPY); de.consume();
    anEvent.acceptDrag(); anEvent.consume();
    
    // Handle DragEnter, DragOver, DragExit: Apply/clear effect from DragEffectNode
    if(anEvent.isDragEnter() || anEvent.isDragOver() && _dragOver!=_pane) {
        if(_dragOver!=null) _dragOver.setUnderDrag(false); _pane.setUnderDrag(true); _dragOver = _pane; }
    if(anEvent.isDragExit()) { _pane.setUnderDrag(false); if(_dragOver==_pane) _dragOver = null; }
    
    // Handle DragDropEvent
    if(anEvent.isDragDropEvent() && SupportPane._dragSP!=null) {
        if(_dragOver!=null) _dragOver.setUnderDrag(false); _dragOver = null;
        dropNode(SupportPane._dragSP.getJNode(), anEvent.getX(), anEvent.getY());
        anEvent.dropComplete(); //de.setDropCompleted(true);
    }
}

/**
 * Drops a node at center of part.
 */
protected void dropNode(JNode aJNode) { dropNode(aJNode, getWidth()/2, getHeight()/2); }

/**
 * Drops a node.
 */
protected void dropNode(JNode aNode, double anX, double aY)
{
    System.out.println("Cannot add node to " + getClass().getSimpleName());
}
    
}