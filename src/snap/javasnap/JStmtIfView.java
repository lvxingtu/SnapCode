package snap.javasnap;
import snap.javakit.*;
import snap.view.*;

/**
 * SnapPartStmt subclass for JStmtIf.
 */
public class JStmtIfView <JNODE extends JStmtIf> extends JStmtView <JNODE> {

/**
 * Updates UI for top line.
 */
protected void updateHBox(HBox theHBox)
{
    JStmtIf istmt = getJNode(); JExpr cond = istmt.getConditional();
    JExprView spart = new JExprEditorView(); spart.setJNode(cond);
    Label label = createLabel("if");
    theHBox.setChildren(label, spart);
}

}