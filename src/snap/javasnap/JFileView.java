package snap.javasnap;
import java.util.*;
import snap.gfx.*;
import snap.javaparse.*;
import snap.view.*;

/**
 * A SnapPart for JFile.
 */
public class JFileView extends JNodeView <JFile> {

    // The SnapCodeArea
    SnapEditor       _codeArea;

/**
 * Returns the SnapCodeArea.
 */
public SnapEditor getCodeArea()  { return _codeArea; }

/**
 * Sets the JNode.
 */
public void setJNode(JFile aJNode)
{
    super.setJNode(aJNode);
    
    // Reset children and their UI
    _children = null;
    _pane.getVBox().removeChildren();
    for(JNodeView child : getChildren())
        _pane.getVBox().addChild(child.getUI());
}

/**
 * Override to return JFile child node owners.
 */
protected List <JNodeView> createChildren()
{
    List <JNodeView> children = new ArrayList();
    JFile jfile = getJNode();
    JClassDecl cdecl = jfile.getClassDecl(); if(cdecl==null) return children;
    for(JMemberDecl md : cdecl.getMemberDecls()) {
        JNodeView mdp = JMemberDeclView.createSnapPart(md); if(mdp==null) continue;
        children.add(mdp);
    }
    return children;
}

/**
 * Creates UI.
 */
public View createUI()
{
    // Get pane and set Type=None
    JNodeViewBase pane = (JNodeViewBase)super.createUI(); pane.setType(JNodeViewBase.Type.None);
    pane.setFill(Color.GRAY); pane.setBorder(Color.LIGHTGRAY, 1); //Bevel
    
    // Configure VBox special for file
    VBox vbox = pane.getVBox(); vbox.setPadding(0,10,10,10); vbox.setSpacing(25);
    vbox.setFillWidth(false);

    // Return pane
    return pane;
}

/**
 * Override to return false.
 */
public boolean isBlock()  { return false; }

/**
 * Returns a string describing the part.
 */
public String getPartString()  { return "Class"; }

}