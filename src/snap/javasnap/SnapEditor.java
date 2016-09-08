package snap.javasnap;
import java.util.List;
import snap.gfx.TextBoxLine;
import snap.javaparse.*;
import snap.javatext.JavaTextView;
import snap.view.*;

/**
 * The Pane that actually holds SnapPart pieces.
 */
public class SnapEditor extends StackView {

    // The JavaTextView
    JavaTextView        _jtextView;
    
    // The scripts pane
    SnapPartFile        _filePart;

    // The selected part
    SnapPart            _selPart;
    
    // The mouse node and X/Y during mouse drag
    View                _mnode;  SnapPart _mpart; double _mx, _my;

/**
 * Creates a new SnapCodeArea.
 */
public SnapEditor(JavaTextView aJTV)
{
    // Set JavaTextView
    _jtextView = aJTV;
    
    // Create FilePart and add
    _filePart = new SnapPartFile(); _filePart._codeArea = this;
    View fpUI = _filePart.getUI(); fpUI.setGrowWidth(true); fpUI.setGrowHeight(true);
    addChild(fpUI);

    // Configure mouse handling
    enableEvents(MousePressed, MouseDragged, MouseReleased);
    rebuildUI();
}

/**
 * Returns the SnapEditorPane.
 */
public SnapEditorPane getEditorPane()  { return getOwner(SnapEditorPane.class); }

/**
 * Returns the JavaTextView.
 */
public JavaTextView getJavaTextView()  { return _jtextView; }

/**
 * Returns the selected part.
 */
public SnapPart getSelectedPart()  { return _selPart; }

/**
 * Sets the selected parts.
 */
public void setSelectedPart(SnapPart aPart)
{
    if(_selPart!=null) _selPart.setSelected(false);
    _selPart = aPart!=null? aPart : _filePart;
    _selPart.setSelected(true);

    // Update JavaTextView selection    
    JNode jnode = _selPart.getJNode();
    int ss = jnode.getStart(), se = jnode.getEnd();
    getJavaTextView().setSel(ss, se);

    // Forward to editor
    SnapEditorPane ep = getEditorPane();
    if(ep!=null) ep.updateSelectedPart(_selPart);
}

/**
 * Returns the FilePart.
 */
public SnapPartFile getFilePart()  { return _filePart; }

/**
 * Returns the JFile JNode.
 */
public JFile getJFile()  { return getJavaTextView().getJFile(); }

/**
 * Returns the selected part's class.
 */
public Class getSelectedPartClass()
{
    // Get class for SnapPart.JNode
    SnapPart spart = getSelectedPart(); if(spart==null) spart = getFilePart();
    JNode jnode = spart.getJNode(); Class cls = null;
    for(JNode jn=jnode; jn!=null && cls==null; jn=jn.getParent())
        cls = jn.getJClass();
    return cls;
}

/**
 * Returns the selected part's class or the enclosing class, if void.class.
 */
public Class getSelectedPartEnclClass()
{
    // Get class for SnapPart.JNode
    SnapPart spart = getSelectedPart(); if(spart==null) spart = getFilePart();
    JNode jnode = spart.getJNode(); Class cls = null;
    for(JNode jn=jnode; jn!=null && (cls==null || cls.isPrimitive()); jn=jn.getParent())
        cls = jn.getJClass();
    return cls;
}

/**
 * Rebuilds the pieces.
 */
protected void rebuildUI()
{
    JFile jfile = getJFile();
    getFilePart().setJNode(jfile);
    setSelectedPartFromTextArea();
}

/**
 * Sets the selected part from TextArea selection.
 */
void setSelectedPartFromTextArea()
{
    int index = getJavaTextView().getSelStart();
    SnapPart spart = getSnapPartAt(getFilePart(), index);
    setSelectedPart(spart);
}

/**
 * Returns the snap part at given index.
 */
public SnapPart getSnapPartAt(SnapPart aPart, int anIndex)
{
    // Check children
    List <SnapPart> children = aPart.getChildren();
    for(SnapPart child : children) {
        SnapPart part = getSnapPartAt(child, anIndex);
        if(part!=null)
            return part;
    }
    
    // Check part
    JNode jnode = aPart.getJNode();
    return jnode.getStart()<=anIndex && anIndex<=jnode.getEnd()? aPart : null;
}

/**
 * Replaces a string.
 */
protected void replaceText(String aString, int aStart, int anEnd)
{
    JavaTextView tview = getJavaTextView();
    tview.undoerSaveChanges();
    tview.replaceChars(aString, null, aStart, anEnd, true);
}

/**
 * Sets text selection.
 */
protected void setTextSelection(int aStart, int anEnd)
{
    JavaTextView tview = getJavaTextView();
    tview.setSel(aStart, anEnd);
}

/**
 * Insets a node.
 */
public void insertNode(JNode aBaseNode, JNode aNewNode, int aPos)
{
    if(aBaseNode instanceof JFile) { System.out.println("Can't add to file"); return; }
    
    if(aBaseNode instanceof JStmtExpr && aNewNode instanceof JStmtExpr &&
        aBaseNode.getJClass()==getSelectedPartClass() && aBaseNode.getJClass()!=void.class) {
        int index = aBaseNode.getEnd();
        String nodeStr = aNewNode.getString(), str = '.' + nodeStr;
        replaceText(str, index - 1, index);
        setTextSelection(index, index + nodeStr.length());
    }
    
    else {
        int index = aPos<0? getBeforeNode(aBaseNode) : aPos>0? getAfterNode(aBaseNode) : getInNode(aBaseNode);
        String indent = getIndent(aBaseNode, aPos);
        String nodeStr = aNewNode.getString().replace("\n", "\n" + indent);
        String str = indent + nodeStr;
        replaceText(str + '\n', index, index);
        setTextSelection(index + indent.length(), index + indent.length() + nodeStr.trim().length());
    }
}

/**
 * Replaces a JNode with string.
 */
public void replaceJNode(JNode aNode, String aString)
{
    replaceText(aString, aNode.getStart(), aNode.getEnd());
}

/**
 * Removes a node.
 */
public void removeNode(JNode aNode)
{
    int start = getBeforeNode(aNode), end = getAfterNode(aNode);
    replaceText(null, start, end);
}

/**
 * Returns after node.
 */
public int getBeforeNode(JNode aNode)
{
    int index = aNode.getStart();
    JExpr pexpr = aNode instanceof JExpr? ((JExpr)aNode).getParentExpr() : null; if(pexpr!=null) return pexpr.getEnd();
    TextBoxLine tline = getJavaTextView().getLineAt(index);
    return tline.getStart();
}

/**
 * Returns after node.
 */
public int getAfterNode(JNode aNode)
{
    int index = aNode.getEnd();
    JExprChain cexpr = aNode.getParent() instanceof JExprChain? (JExprChain)aNode.getParent() : null;
    if(cexpr!=null) return cexpr.getExpr(cexpr.getExprCount()-1).getEnd();
    TextBoxLine tline = getJavaTextView().getLineAt(index);
    return tline.getEnd();
}

/**
 * Returns in the node.
 */
public int getInNode(JNode aNode)
{
    JavaTextView tview = getJavaTextView();
    int index = aNode.getStart(); while(index<tview.length() && tview.charAt(index)!='{') index++;
    TextBoxLine tline = tview.getLineAt(index);
    return tline.getEnd();
}

/**
 * Returns the indent.
 */
String getIndent(JNode aNode, int aPos)
{
    int index = aNode.getStart();
    TextBoxLine tline = getJavaTextView().getLineAt(index);
    int c = 0; while(c<tline.length() && Character.isWhitespace(tline.charAt(c))) c++;
    StringBuffer sb = new StringBuffer(); for(int i=0;i<c;i++) sb.append(' ');
    if(aPos==0) sb.append("    ");
    return sb.toString();
}

/**
 * Process events.
 */
protected void processEvent(ViewEvent anEvent)
{
    // Handle MousePressed
    if(anEvent.isMousePressed()) {
        _mx = anEvent.getX(); _my = anEvent.getY();
        _mnode = ViewUtils.getDeepestChildAt(this, _mx, _my);
        _mpart = SnapPart.getSnapPart(_mnode);
        if(_mpart==null) _mnode = null; else _mnode = _mpart.getUI();
        if(_mpart==_filePart) { setSelectedPart(null); _mpart = null; }
        setSelectedPart(_mpart);
    }
    
    // Handle MouseDragged
    else if(anEvent.isMouseDragged()) {
        if(_mpart==null) return;
        double mx = anEvent.getX(), my = anEvent.getY();
        _mnode.setTransX(_mnode.getTransX() + mx - _mx); _mx = mx;
        _mnode.setTransY(_mnode.getTransY() + my - _my); _my = my;
    }
    
    // Handle MouseReleased
    else if(anEvent.isMouseReleased()) {
        if(_mpart==null) return;
        if(_mnode.getTransX()>150 && _mpart.getParent()!=null) removeNode(_mpart.getJNode());
        _mnode.setTransX(0); _mnode.setTransY(0);
        _mnode = null;
    }
}

}