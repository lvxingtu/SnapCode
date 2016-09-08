package snap.javasnap;
import snap.javaparse.JNode;
import snap.view.*;

/**
 * The Pane that actually holds SnapPart pieces.
 */
public class SnapEditor extends StackView {

    // The scripts pane
    SnapPartFile        _filePart;

    // The selected part
    SnapPart            _selPart;
    
    // The mouse node and X/Y during mouse drag
    View                _mnode;  SnapPart _mpart; double _mx, _my;

/**
 * Creates a new SnapCodeArea.
 */
public SnapEditor()
{
    // Create FilePart and add
    _filePart = new SnapPartFile(); _filePart._codeArea = this;
    View fpUI = _filePart.getUI(); fpUI.setGrowWidth(true); fpUI.setGrowHeight(true);
    addChild(fpUI);

    // Configure mouse handling
    enableEvents(MousePressed, MouseDragged, MouseReleased);
}

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
    
    // Forward to editor
    updateSelectedPart(_selPart);
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

/**
 * Sets the selected part.
 */
protected void updateSelectedPart(SnapPart aPart)  { }

/**
 * Insets a node.
 */
public void insertNode(JNode aBaseNode, JNode aNewNode, int aPos)  { }

/**
 * Replaces a JNode with string.
 */
public void replaceJNode(JNode aNode, String aString)  { }

/**
 * Removes a node.
 */
public void removeNode(JNode aNode)  { }

}