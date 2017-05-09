package snap.javasnap;
import snap.javakit.*;
import snap.view.*;

/**
 * A SnapPart subclass for JMethodDecl.
 */
public class JMemberDeclView <JNODE extends JMemberDecl> extends JNodeView <JNODE> {
    
/**
 * Creates a SnapPart for a JNode.
 */
public static JNodeView createView(JNode aNode)
{
    JNodeView np = null;
    if(aNode instanceof JConstrDecl) np = new ConstructorDecl();
    else if(aNode instanceof JMethodDecl) np = new MethodDecl();
    else return null;
    np.setJNode(aNode);
    return np;
}

/**
 * Subclass for JMethodDecl.
 */
public static class MethodDecl <JNODE extends JMethodDecl> extends JMemberDeclView <JNODE> {

    /** Override. */
    protected void updateUI()
    {
        super.updateUI(); setType(JNodeViewBase.Type.MemberDecl);
        setColor(MemberDeclColor); getHBox().setMinWidth(120);
    }
    
    /** Updates UI for HBox. */
    protected void updateHBox(HBox spane)
    {
        JMethodDecl md = getJNode();
        Label label = createLabel(md.getName()); label.setFont(label.getFont().deriveFont(14));
        spane.addChild(label);
    }
    
    /** Returns a string describing the part. */
    public String getPartString()  { return "Method"; }
    
    /** Drops a node. */
    protected void dropNode(JNode aNode, double anX, double aY)
    {
        if(getJNodeViewCount()==0) getCodeArea().insertNode(getJNode(), aNode, 0);
        else if(aY<getHeight()/2) getJNodeView(0).dropNode(aNode, anX, 0);
        else getJNodeViewLast().dropNode(aNode, anX, getJNodeViewLast().getHeight());
    }
}

/**
 * Subclass for JConstructorDecl.
 */
public static class ConstructorDecl <JNODE extends JConstrDecl> extends MethodDecl <JNODE> {

    /** Returns a string describing the part. */
    public String getPartString()  { return "Constructor"; }
}
    
}