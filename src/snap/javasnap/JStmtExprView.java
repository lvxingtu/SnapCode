package snap.javasnap;
import java.util.*;
import snap.javakit.*;
import snap.view.*;

/**
 * JStmtView subclass for JStmtExpression.
 */
public class JStmtExprView <JNODE extends JStmtExpr> extends JStmtView <JNODE> {

/**
 * Updates UI for HBox.
 */
protected void updateHBox(HBox aHBox)
{
    for(JNodeView spart : getJNodeViews()) aHBox.addChild(spart);
    for(View child : aHBox.getChildren()) child.setGrowWidth(true);
}

/**
 * Override to return JFile child node owners.
 */
protected List <JNodeView> createJNodeViews()
{
    JStmtExpr stmt = getJNode(); JExpr expr = stmt.getExpr();
    JExprView sexpr = JExprView.createView(expr);
    List <JNodeView> children = new ArrayList(); children.add(sexpr);
    return children;
}

}