package snap.javasnap;
import snap.javaparse.*;
import snap.javasnap.JNodeViewBase.Seg;
import snap.view.View;

/**
 * A SnapPart subclass for JExpr.
 */
public abstract class JExprView <JNODE extends JExpr> extends JNodeView <JNODE> {

/**
 * Creates a SnapPart for a JNode.
 */
public static JExprView createSnapPart(JNode aNode)
{
    JExprView sp;
    if(aNode instanceof JExprMethodCall) sp = new JExprMethodCallView();
    else if(aNode instanceof JExprChain) sp = new JExprChainView();
    else sp = new JExprEditorView();
    sp.setJNode(aNode);
    return sp;
}

/**
 * Returns whether expression is in chain.
 */
public boolean isChained()  { return getParent() instanceof JExprChainView; }

/**
 * Creates UI.
 */
public View createUI()
{
    JNodeViewBase pane = (JNodeViewBase)super.createUI(); pane.setPadding(0,2,2,4);
    
    if(isChained()) { JNodeView par = getParent();
        pane.setSeg(this==par.getChild(0)? Seg.First : this==par.getChildLast()? Seg.Last : Seg.Middle);
        pane.setColor(PieceColor);
    }

    return pane;
}

/**
 * Override to forward to parent.
 */
protected void dropNode(JNode aJNode, double anX, double aY)
{
    getParent().dropNode(aJNode, anX, aY);
}

}