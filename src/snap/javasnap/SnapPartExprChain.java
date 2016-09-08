package snap.javasnap;
import java.util.*;
import snap.javaparse.*;
import snap.view.*;

/**
 * SnapPartExpr subclass for JExprChain.
 */
public class SnapPartExprChain <JNODE extends JExprChain> extends SnapPartExpr <JNODE> {

/**
 * Override to create children.
 */
protected List <SnapPart> createChildren()
{
    JExprChain echain = getJNode();
    List children = new ArrayList();
    for(JExpr exp : echain.getExpressions()) { SnapPartExpr spe = createSnapPart(exp); children.add(spe); }
    return children;
}

/**
 * Configure HBox.
 */
protected void configureHBox(HBox aHBox)
{
    for(SnapPart child : getChildren()) aHBox.addChild(child.getUI());
    for(View child : aHBox.getChildren()) child.setGrowWidth(true);
}

}