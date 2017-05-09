package snap.javasnap;
import java.util.*;
import snap.javakit.*;
import snap.view.*;

/**
 * SnapPartExpr subclass for JExprChain.
 */
public class JExprChainView <JNODE extends JExprChain> extends JExprView <JNODE> {

/**
 * Updates HBox.
 */
protected void updateHBox(HBox aHBox)
{
    for(JNodeView child : getJNodeViews()) aHBox.addChild(child);
    for(View child : aHBox.getChildren()) child.setGrowWidth(true);
    getJNodeView(0).setSeg(Seg.First);
    getJNodeViewLast().setSeg(Seg.Last);
}

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

}