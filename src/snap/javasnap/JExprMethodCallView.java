package snap.javasnap;
import java.util.*;
import snap.javaparse.*;
import snap.view.*;

/**
 * SnapPartExpr subclass for JMethodCall.
 */
public class JExprMethodCallView <JNODE extends JExprMethodCall> extends JExprView <JNODE> {

/**
 * Override to create children for method args.
 */
protected List <JNodeView> createChildren()
{
    JExprMethodCall mc = getJNode(); List <JExpr> args = mc.getArgs();
    List children = new ArrayList();
    if(args!=null) for(JExpr arg : args) { JExprView spe = new JExprEditorView(); spe.setJNode(arg);
        children.add(spe); }
    return children;
}

/**
 * Override.
 */
public View createUI()
{
    JNodeViewBase pane = (JNodeViewBase)super.createUI(); pane.setColor(PieceColor);
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
    for(JNodeView child : getChildren())
        aHBox.addChild(child.getUI());
}

}