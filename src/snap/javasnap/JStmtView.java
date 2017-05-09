package snap.javasnap;
import snap.javakit.*;
import snap.view.*;

/**
 * A JNodeView for JStatement.
 */
public class JStmtView <JNODE extends JStmt> extends JNodeView <JNODE> {

/**
 * Override to configure.
 */
protected void updateUI()
{
    super.updateUI();
    setType(isBlock()? JNodeViewBase.Type.BlockStmt : JNodeViewBase.Type.Piece);
    setColor(isBlock()? BlockStmtColor : PieceColor);
    getHBox().setMinWidth(120);
}

/**
 * Updates UI for top line.
 */
protected void updateHBox(HBox theHBox)
{
    // Create label for statement and add to HBox
    JStmt stmt = getJNode();
    Label label = createLabel(stmt.getString());
    theHBox.addChild(label);
}

/**
 * Returns a string describing the part.
 */
public String getPartString()  { return getJNode().getClass().getSimpleName().substring(5) + " Statement"; }

/**
 * Drops a node.
 */
protected void dropNode(JNode aNode, double anX, double aY)
{
    // If not statement, bail
    if(!(aNode instanceof JStmt)) {
        System.out.println("SnapPartStmt.dropNode: Can't drop " + aNode); return; }

    // If less than 11, insert node before statement
    if(aY<11)
        getCodeArea().insertNode(getJNode(), aNode, -1);
    
    // If greater than Height-6 or simple statement, insert node after statement
    else if(aY>getHeight()-6 || !isBlock())
        getCodeArea().insertNode(getJNode(), aNode, 1);
    
    // If block but no children, insert inside statement
    else if(getJNodeViewCount()==0)
        getCodeArea().insertNode(getJNode(), aNode, 0);
    
    // If before first child statement, have first child dropNode, otherwise have last child dropNode 
    else if(aY<getHeight()/2)
        getJNodeView(0).dropNode(aNode, anX, 0);
    else getJNodeViewLast().dropNode(aNode, anX, getJNodeViewLast().getHeight());
}
    
/**
 * Creates a JStmtView for a JStmt.
 */
public static JNodeView createView(JNode aNode)
{
    if(aNode instanceof JStmtExpr) return new JStmtExprView();
    if(aNode instanceof JStmtWhile) return new JStmtWhileView();
    if(aNode instanceof JStmtIf) return new JStmtIfView();
    if(aNode instanceof JStmtFor) return new JStmtForView();
    return new JStmtView();
}

}