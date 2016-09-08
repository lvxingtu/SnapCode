package snap.javasnap;
import snap.javaparse.*;
import snap.view.*;

/**
 * SnapPartStmt subclass for JStmtWhile.
 */
public class SnapPartStmtWhile <JNODE extends JStmtWhile> extends SnapPartStmt <JNODE> {

/**
 * Creates the UI for the top line.
 */
protected void configureHBox(HBox theHBox)
{
    JStmtWhile wstmt = getJNode(); JExpr cond = wstmt.getConditional();
    SnapPartExpr spart = new SnapPartExprEditor(); spart.setJNode(cond); spart._parent = this;
    Label label = createLabel("while");
    theHBox.setChildren(label, spart.getUI());
}    

}