package snap.javasnap;
import snap.javaparse.*;

/**
 * A JNodeView subclass for JExpr.
 */
public abstract class JExprView <JNODE extends JExpr> extends JNodeView <JNODE> {

/**
 * Updates UI.
 */
public void updateUI()
{
    super.updateUI(); setPadding(0,2,2,4);
    
    if(isChained()) { JNodeView par = getJNodeViewParent();
        setSeg(this==par.getJNodeView(0)? Seg.First : this==par.getJNodeViewLast()? Seg.Last : Seg.Middle);
        setColor(PieceColor);
    }
}

/**
 * Returns whether expression is in chain.
 */
public boolean isChained()  { return getJNodeViewParent() instanceof JExprChainView; }

/**
 * Override to forward to parent.
 */
protected void dropNode(JNode aJNode, double anX, double aY)
{
    getJNodeViewParent().dropNode(aJNode, anX, aY);
}

/**
 * Creates a JNodeView for a JNode.
 */
public static JExprView createView(JNode aNode)
{
    JExprView sp;
    if(aNode instanceof JExprMethodCall) sp = new JExprMethodCallView();
    else if(aNode instanceof JExprChain) sp = new JExprChainView();
    else sp = new JExprEditorView();
    sp.setJNode(aNode);
    return sp;
}

}