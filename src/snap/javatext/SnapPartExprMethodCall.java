package snap.javatext;
import java.util.*;
import snap.javaparse.*;
import snap.view.*;

/**
 * SnapPartExpr subclass for JMethodCall.
 */
public class SnapPartExprMethodCall <JNODE extends JExprMethodCall> extends SnapPartExpr <JNODE> {

/**
 * Override to create children for method args.
 */
protected List <SnapPart> createChildren()
{
    JExprMethodCall mc = getJNode(); List <JExpr> args = mc.getArgs();
    List children = new ArrayList();
    if(args!=null) for(JExpr arg : args) { SnapPartExpr spe = new SnapPartExprEditor(); spe.setJNode(arg);
        children.add(spe); }
    return children;
}

/**
 * Override.
 */
public View createUI()
{
    SnapPartPane pane = (SnapPartPane)super.createUI(); pane.setColor(PieceColor);
    return pane;
}

/**
 * Configure HBox.
 */
protected void configureHBox(HBox aHBox)
{
    // Add label for method name
    JExprMethodCall mc = getJNode();
    Label label = createLabel(mc.getName());
    aHBox.addChild(label);
    
    // Add child UIs
    for(SnapPart child : getChildren())
        aHBox.addChild(child.getUI());
}

}