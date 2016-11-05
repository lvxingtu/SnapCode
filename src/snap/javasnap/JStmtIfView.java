package snap.javasnap;
import snap.javaparse.*;
import snap.view.*;

/**
 * SnapPartStmt subclass for JStmtIf.
 */
public class JStmtIfView <JNODE extends JStmtIf> extends JStmtView <JNODE> {

/**
 * Creates the UI for the top line.
 */
protected void configureHBox(HBox theHBox)
{
    JStmtIf istmt = getJNode(); JExpr cond = istmt.getConditional();
    JExprView spart = new JExprEditorView(); spart.setJNode(cond); spart._parent = this;
    Label label = createLabel("if");
    theHBox.setChildren(label, spart.getUI());
}

}