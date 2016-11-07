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
    _jnodeViews = null;
    getVBox().removeChildren();
    for(JNodeView child : getJNodeViews())
        getVBox().addChild(child);
}

/**
 * Updates UI.
 */
public void updateUI()
{
    // Get pane and set Type=None
    super.updateUI(); setType(JNodeViewBase.Type.None);
    setFill(Color.GRAY); setBorder(Color.LIGHTGRAY, 1); //Bevel
    
    // Configure VBox special for file
    VBox vbox = getVBox(); vbox.setPadding(0,10,10,10); vbox.setSpacing(25);
    vbox.setFillWidth(false);
}

/**
 * Override to return JFile child node owners.
 */
protected List <JNodeView> createJNodeViews()
{
    List <JNodeView> children = new ArrayList();
    JFile jfile = getJNode();
    JClassDecl cdecl = jfile.getClassDecl(); if(cdecl==null) return children;
    for(JMemberDecl md : cdecl.getMemberDecls()) {
        JNodeView mdp = JMemberDeclView.createView(md); if(mdp==null) continue;
        children.add(mdp);
    }
    return children;
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