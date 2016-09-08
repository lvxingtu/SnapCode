package snap.javasnap;
import java.util.*;
import snap.gfx.*;
import snap.javaparse.*;
import snap.javatext.JavaTextView;
import snap.view.*;

/**
 * This class manages a SnapCodeArea.
 */
public class SnapEditorPane extends ViewOwner {

    // The main editor UI
    SnapEditor          _editor;

    // A class for editing code
    SnapJavaPane        _javaPane;
    
    // The SnapCodePane that owns this editor
    SnapCodePane        _codePane;
    
    // The pieces pane
    SupportPane         _supportPane;
    
    // The node path
    HBox                _nodePathBox;
    
    // The deepest part of current NodePath (which is SelectedPart, unless NodePath changed SelectedPart)
    SnapPart            _deepPart;

    // Whether to rebuild CodeArea
    boolean             _rebuild = true;
    
/**
 * Returns the SnapEditor.
 */
public SnapEditor getEditor()  { return _editor; }

/**
 * Returns the SnapJavaPane.
 */
public SnapJavaPane getJavaPane()  { return _javaPane; }

/**
 * Returns the JavaTextView.
 */
public JavaTextView getJavaView()  { return _javaPane.getTextView(); }

/**
 * Returns the SupportPane.
 */
public SupportPane getSupportPane()
{
    if(_supportPane==null) { _supportPane = createSupportPane(); _supportPane._editorPane = this; }
    return _supportPane;
}

/**
 * Creates the SupportPane.
 */
protected SupportPane createSupportPane()  { return new SupportPane(); }

/**
 * Returns the FilePart.
 */
public SnapPartFile getFilePart()  { return _editor._filePart; }

/**
 * Returns the JFile JNode.
 */
public JFile getJFile()  { return getJavaView().getJFile(); }

/**
 * Returns the selected part.
 */
public SnapPart getSelectedPart()  { return _editor.getSelectedPart(); }

/**
 * Sets the selected parts.
 */
public void setSelectedPart(SnapPart aPart)  { _editor.setSelectedPart(aPart); }

/**
 * Returns the selected part's class.
 */
public Class getSelectedPartClass()
{
    // Get class for SnapPart.JNode
    SnapPart spart = getSelectedPart(); if(spart==null) spart = getFilePart();
    JNode jnode = spart.getJNode(); Class cls = null;
    for(JNode jn=jnode; jn!=null && cls==null; jn=jn.getParent())
        cls = jn.getJClass();
    return cls;
}

/**
 * Returns the selected part's class or the enclosing class, if void.class.
 */
public Class getSelectedPartEnclClass()
{
    // Get class for SnapPart.JNode
    SnapPart spart = getSelectedPart(); if(spart==null) spart = getFilePart();
    JNode jnode = spart.getJNode(); Class cls = null;
    for(JNode jn=jnode; jn!=null && (cls==null || cls.isPrimitive()); jn=jn.getParent())
        cls = jn.getJClass();
    return cls;
}

/**
 * Returns the snap part at given index.
 */
public SnapPart getSnapPartAt(SnapPart aPart, int anIndex)
{
    // Check children
    List <SnapPart> children = aPart.getChildren();
    for(SnapPart child : children) {
        SnapPart part = getSnapPartAt(child, anIndex);
        if(part!=null)
            return part;
    }
    
    // Check part
    JNode jnode = aPart.getJNode();
    return jnode.getStart()<=anIndex && anIndex<=jnode.getEnd()? aPart : null;
}

/**
 * Create UI.
 */
protected View createUI()
{
    // Get normal UI
    View toolBar = super.createUI(); //toolBar.setMaxHeight(28);
    
    // Create SnapCodeArea
    _editor = new SnapEditor() {
        protected void updateSelectedPart(SnapPart aPart)  { SnapEditorPane.this.updateSelectedPart(aPart); }
        public void insertNode(JNode aBsNd, JNode aNwNd, int aPos)  { _javaPane.insertNode(aBsNd, aNwNd, aPos); }
        public void replaceJNode(JNode aNode, String aString)  { _javaPane.replaceJNode(aNode, aString); }
        public void removeNode(JNode aNode) { _javaPane.removeNode(aNode); }
    };

    // Add to Editor.UI to ScrollView
    ScrollView sview = new ScrollView(_editor); sview.setGrowWidth(true);

    // Get SupportPane
    _supportPane = getSupportPane(); _supportPane.getUI().setPrefWidth(300);
    
    // Create SplitView, configure and return
    SplitView spane = new SplitView();
    spane.setChildren(sview, _supportPane.getUI());
    
    // Create NodePath and add to bottom
    _nodePathBox = new HBox(); _nodePathBox.setPadding(2,2,2,2);
    
    // Rebuild Area UI
    rebuildUI();
    
    // Create BorderView with toolbar
    BorderView bview = new BorderView(); bview.setCenter(spane); bview.setTop(toolBar); bview.setBottom(_nodePathBox);
    return bview;
}

/**
 * Initialize UI.
 */
protected void initU()
{
    addKeyActionEvent("CutButton", "Shortcut+X");
    addKeyActionEvent("CopyButton", "Shortcut+C");
    addKeyActionEvent("PasteButton", "Shortcut+V");
    addKeyActionEvent("DeleteButton", "DELETE");
    addKeyActionEvent("DeleteButton", "BACKSPACE");
    addKeyActionEvent("UndoButton", "Shortcut+Z");
    addKeyActionEvent("RedoButton", "Shortcut+Shift+Z");
    addKeyActionEvent("Escape", "ESC");
    
    setSelectedPart(null);
}

/**
 * ResetUI.
 */
public void resetUI()
{
    if(_rebuild) rebuildUI();
    rebuildNodePath();
}

/**
 * Respond to UI changes.
 */
protected void respondUI(ViewEvent anEvent)
{
    // Handle JavaButton
    if(anEvent.equals("JavaButton"))
        _codePane.setShowSnapCode(false);
    
    // Handle NodePathLabel
    if(anEvent.equals("NodePathLabel")) {
        Label label = anEvent.getView(Label.class);
        SnapPart part = (SnapPart)label.getProp("SnapPart"), dpart = _deepPart;
        setSelectedPart(part);
        _deepPart = dpart;
    }
    
    // Handle SaveButton
    if(anEvent.equals("SaveButton")) getJavaPane().saveChanges();
    
    // Handle CutButton, CopyButton, PasteButton, Escape
    if(anEvent.equals("CutButton")) cut();
    if(anEvent.equals("CopyButton")) copy();
    if(anEvent.equals("PasteButton")) paste();
    if(anEvent.equals("DeleteButton")) delete();
    if(anEvent.equals("Escape")) escape();
    
    // Handle UndoButton, RedoButton
    if(anEvent.equals("UndoButton")) undo();
    if(anEvent.equals("RedoButton")) redo();
}

/**
 * Rebuilds the NodePathBox.
 */
void rebuildNodePath()
{
    // Clear path and get font
    _nodePathBox.removeChildren();
    
    // Iterate up from DeepPart and add parts
    for(SnapPart part=_deepPart, spart=getSelectedPart(); part!=null;) {
        Label label = new Label(part.getPartString()); label.setFont(Font.Arial12);
        label.setName("NodePathLabel"); label.setProp("SnapPart", part);
        if(part==spart) label.setFill(Color.LIGHTGRAY);
        _nodePathBox.addChild(label,0); label.setOwner(this); enableEvents(label, MouseClicked);
        part = part.getParent(); if(part==null) break;
        Label div = new Label(" \u2022 "); div.setFont(Font.Arial12);
        _nodePathBox.addChild(div,0);
    }
}

/**
 * Rebuilds the CodeArea UI.
 */
protected void rebuildUI()
{
    JFile jfile = getJFile();
    getFilePart().setJNode(jfile);
    setSelectedPartFromTextArea();
    _rebuild = false;
}

/**
 * Rebuilds the CodeArea UI later.
 */
protected void rebuildLater()  { _rebuild = true; resetLater(); }

/**
 * Sets the selected parts.
 */
public void updateSelectedPart(SnapPart aPart)
{
    _supportPane.rebuildUI();
    
    //
    JNode jnode = aPart.getJNode();
    int ss = jnode.getStart(), se = jnode.getEnd();
    getJavaView().setSel(ss, se);
    
    //
    resetLater();
    _deepPart = aPart;
}

/**
 * Sets the selected part from TextArea selection.
 */
void setSelectedPartFromTextArea()
{
    int index = getJavaView().getSelStart();
    SnapPart spart = getSnapPartAt(getFilePart(), index);
    setSelectedPart(spart);
}

/**
 * Cut current selection to clipboard.
 */
public void cut()  { copy(); delete(); }

/**
 * Copy current selection to clipboard.
 */
public void copy()
{
    // Make sure statement is selected
    if(!(getSelectedPart() instanceof SnapPartStmt)) {
        SnapPartStmt stmt = (SnapPartStmt)getSelectedPart().getAncestor(SnapPartStmt.class); if(stmt==null) return;
        setSelectedPart(stmt);
    }
    
    // Do copy
    getJavaView().copy();
}

/**
 * Paste ClipBoard contents.
 */
public void paste()
{
    // Get Clipboard String and create node
    Clipboard cb = Clipboard.get(); if(!cb.hasString()) return;
    String str = cb.getString();
    JNode node = null;
    try { node = _supportPane._stmtParser.parseCustom(str, JNode.class); } catch(Exception e) { }
    if(node==null)
        try { node = _supportPane._exprParser.parseCustom(str, JNode.class); } catch(Exception e) { }
    
    // Get SelectedPart and drop node
    SnapPart spart = getSelectedPart();
    if(spart!=null && node!=null)
        spart.dropNode(node, spart.getWidth()/2, spart.getHeight());
}

/**
 * Delete current selection.
 */
public void delete()  { getJavaView().delete(); rebuildLater(); }

/**
 * Undo last change.
 */
public void undo()  { getJavaView().undo(); rebuildLater(); }

/**
 * Redo last undo.
 */
public void redo()  { getJavaView().redo(); rebuildLater(); }

/**
 * Escape.
 */
public void escape()  { SnapPart par = getSelectedPart().getParent(); if(par!=null) setSelectedPart(par); }

}