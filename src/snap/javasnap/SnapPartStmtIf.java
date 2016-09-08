package snap.javasnap;
import snap.javaparse.*;
import snap.view.*;

/**
 * SnapPartStmt subclass for JStmtIf.
 */
public class SnapPartStmtIf <JNODE extends JStmtIf> extends SnapPartStmt <JNODE> {

/**
 * Creates the UI for the top line.
 */
protected void configureHBox(HBox theHBox)
{
    JStmtIf istmt = getJNode(); JExpr cond = istmt.getConditional();
    SnapPartExpr spart = new SnapPartExprEditor(); spart.setJNode(cond); spart._parent = this;
    Label label = createLabel("if");
    theHBox.setChildren(label, spart.getUI());
}

}