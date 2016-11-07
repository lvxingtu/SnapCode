package snap.javasnap;
import snap.javaparse.*;
import snap.view.*;

/**
 * SnapPartStmt subclass for JStmtWhile.
 */
public class JStmtWhileView <JNODE extends JStmtWhile> extends JStmtView <JNODE> {

/**
 * Updates UI for top line.
 */
protected void updateHBox(HBox theHBox)
{
    JStmtWhile wstmt = getJNode(); JExpr cond = wstmt.getConditional();
    JExprView spart = new JExprEditorView(); spart.setJNode(cond);
    Label label = createLabel("while");
    theHBox.setChildren(label, spart);
}    

}