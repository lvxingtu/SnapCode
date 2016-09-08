package snap.javasnap;
import snap.javaparse.*;
import snap.view.*;

/**
 * A SnapPart subclass for JMethodDecl.
 */
public class SnapPartMemberDecl <JNODE extends JMemberDecl> extends SnapPart <JNODE> {

/**
 * Creates a SnapPart for a JNode.
 */
public static SnapPart createSnapPart(JNode aNode)
{
    SnapPart np = null;
    if(aNode instanceof JConstrDecl) np = new ConstructorDecl();
    else if(aNode instanceof JMethodDecl) np = new MethodDecl();
    else return null;
    np.setJNode(aNode);
    return np;
}

/**
 * Subclass for JMethodDecl.
 */
public static class MethodDecl <JNODE extends JMethodDecl> extends SnapPartMemberDecl <JNODE> {

    /**
     * Override to configure SnapPartPane.
     */
    protected View createUI()
    {
        SnapPartPane pane = (SnapPartPane)super.createUI(); pane.setType(SnapPartPane.Type.MemberDecl);
        pane.setColor(MemberDeclColor); pane.getHBox().setMinWidth(120);
        return pane;
    }
    
    /**
     * Creates UI.
     */
    protected void configureHBox(HBox spane)
    {
        JMethodDecl md = getJNode();
        Label label = createLabel(md.getName());
        spane.addChild(label);
    }
    
    /**
     * Returns a string describing the part.
     */
    public String getPartString()  { return "Method"; }
    
    /**
     * Drops a node.
     */
    protected void dropNode(JNode aNode, double anX, double aY)
    {
        if(getChildCount()==0) getCodeArea().insertNode(getJNode(), aNode, 0);
        else if(aY<getHeight()/2) getChild(0).dropNode(aNode, anX, 0);
        else getChildLast().dropNode(aNode, anX, getChildLast().getHeight());
    }
}

/**
 * Subclass for JConstructorDecl.
 */
public static class ConstructorDecl <JNODE extends JConstrDecl> extends MethodDecl <JNODE> {

    /**
     * Returns a string describing the part.
     */
    public String getPartString()  { return "Constructor"; }
}
    
}