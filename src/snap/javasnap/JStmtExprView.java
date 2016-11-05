package snap.javasnap;
import java.util.*;
import snap.javaparse.*;
import snap.view.*;

/**
 * SnapPartStmt subclass for JStmtExpression.
 */
public class JStmtExprView <JNODE extends JStmtExpr> extends JStmtView<JNODE> {

/**
 * Override to return JFile child node owners.
 */
protected List <JNodeView> createChildren()
{
    JStmtExpr stmt = getJNode(); JExpr expr = stmt.getExpr();
    JExprView sexpr = JExprView.createSnapPart(expr);
    List <JNodeView> children = new ArrayList(); children.add(sexpr);
    return children;
}

/**
 * Override to configure SnapPartPane.
 */
protected View createUI()
{
    JNodeViewBase pane = (JNodeViewBase)super.createUI(); pane.setColor(null);
    return pane;
}

/**
 * Creates UI.
 */
protected void configureHBox(HBox aHBox)
{
    for(JNodeView spart : getChildren()) aHBox.addChild(spart.getUI());
    for(View child : aHBox.getChildren()) child.setGrowWidth(true);
}

}