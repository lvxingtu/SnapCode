package snap.javasnap;
import java.util.*;
import snap.javaparse.*;
import snap.view.*;

/**
 * SnapPartExpr subclass for JExprChain.
 */
public class JExprChainView <JNODE extends JExprChain> extends JExprView <JNODE> {

/**
 * Override to create children.
 */
protected List <JNodeView> createJNodeViews()
{
    JExprChain echain = getJNode();
    List children = new ArrayList();
    for(JExpr exp : echain.getExpressions()) { JExprView spe = createView(exp); children.add(spe); }
    return children;
}

/**
 * Configure HBox.
 */
protected void configureHBox(HBox aHBox)
{
    for(JNodeView child : getJNodeViews()) aHBox.addChild(child.getUI());
    for(View child : aHBox.getChildren()) child.setGrowWidth(true);
}

}