package snap.javatext;
import java.util.*;
import snap.javaparse.*;
import snap.view.*;

/**
 * SnapPartStmt subclass for JStmtExpression.
 */
public class SnapPartStmtExpr <JNODE extends JStmtExpr> extends SnapPartStmt<JNODE> {

/**
 * Override to return JFile child node owners.
 */
protected List <SnapPart> createChildren()
{
    JStmtExpr stmt = getJNode(); JExpr expr = stmt.getExpr();
    SnapPartExpr sexpr = SnapPartExpr.createSnapPart(expr);
    List <SnapPart> children = new ArrayList(); children.add(sexpr);
    return children;
}

/**
 * Override to configure SnapPartPane.
 */
protected View createUI()
{
    SnapPartPane pane = (SnapPartPane)super.createUI(); pane.setColor(null);
    return pane;
}

/**
 * Creates UI.
 */
protected void configureHBox(HBox aHBox)
{
    for(SnapPart spart : getChildren()) aHBox.addChild(spart.getUI());
    for(View child : aHBox.getChildren()) child.setGrowWidth(true);
}

}