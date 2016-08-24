package snap.javatext;
import snap.javaparse.*;
import snap.javatext.SnapPartPane.Seg;
import snap.view.View;

/**
 * A SnapPart subclass for JExpr.
 */
public abstract class SnapPartExpr <JNODE extends JExpr> extends SnapPart <JNODE> {

/**
 * Creates a SnapPart for a JNode.
 */
public static SnapPartExpr createSnapPart(JNode aNode)
{
    SnapPartExpr sp;
    if(aNode instanceof JExprMethodCall) sp = new SnapPartExprMethodCall();
    else if(aNode instanceof JExprChain) sp = new SnapPartExprChain();
    else sp = new SnapPartExprEditor();
    sp.setJNode(aNode);
    return sp;
}

/**
 * Returns whether expression is in chain.
 */
public boolean isChained()  { return getParent() instanceof SnapPartExprChain; }

/**
 * Creates UI.
 */
public View createUI()
{
    SnapPartPane pane = (SnapPartPane)super.createUI(); pane.setPadding(0,2,2,4);
    
    if(isChained()) { SnapPart par = getParent();
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