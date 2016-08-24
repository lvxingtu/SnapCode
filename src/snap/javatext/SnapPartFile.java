package snap.javatext;
import java.util.*;
import snap.gfx.*;
import snap.javaparse.*;
import snap.view.*;

/**
 * A SnapPart for JFile.
 */
public class SnapPartFile extends SnapPart <JFile> {

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
    for(SnapPart child : getChildren())
        _pane.getVBox().addChild(child.getUI());
}

/**
 * Override to return JFile child node owners.
 */
protected List <SnapPart> createChildren()
{
    List <SnapPart> children = new ArrayList();
    JFile jfile = getJNode();
    JClassDecl cdecl = jfile.getClassDecl(); if(cdecl==null) return children;
    for(JMemberDecl md : cdecl.getMemberDecls()) {
        SnapPart mdp = SnapPartMemberDecl.createSnapPart(md); if(mdp==null) continue;
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
    SnapPartPane pane = (SnapPartPane)super.createUI(); pane.setType(SnapPartPane.Type.None);
    pane.setFill(Color.GRAY); pane.setBorder(Border.createLineBorder(Color.LIGHTGRAY,1)); //Bevel
    
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