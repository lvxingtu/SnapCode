/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snap.javatext;
import snap.gfx.*;
import snap.javaparse.*;
import snap.util.*;
import snap.view.*;
import snap.viewx.TextPane;

/**
 * A panel for editing Java files.
 */
public class JavaTextPane extends TextPane {

    // The JavaTextView
    JavaTextView          _textView;
    
    // The SplitView
    SplitView             _splitView;
    
/**
 * Returns the JavaTextView.
 */
public JavaTextView getTextView()  { return (JavaTextView)super.getTextView(); }

/**
 * Creates the JavaTextView.
 */
protected JavaTextView createTextView()  { return new JavaTextView(); }

/**
 * Returns the code completion popup.
 */
public JavaPopupList getPopup()  { return getTextView().getPopup(); }

/**
 * Returns the CodeBuilder.
 */
public CodeBuilder getCodeBuilder()  { return getTextView().getCodeBuilder(); }

/**
 * Returns whether CodeBuilder is visible.
 */
public boolean isCodeBuilderVisible() { return _splitView.getItemCount()>1; }

/**
 * Sets whether CodeBuilder is visible.
 */
public void setCodeBuilderVisible(boolean aFlag)
{
    // If already set, just return
    if(aFlag==isCodeBuilderVisible()) return;
    View codeBuildrPane = getCodeBuilder().getUI();
    
    // If showing CodeBuilder, add to SplitView (animated)
    if(aFlag) {
        _splitView.addChildWithAnim(codeBuildrPane, 260);
        getCodeBuilder().setCodeBlocks();
    }
    
    // If hiding CodeBuilder, remove from SplitView (animated)
    else if(_splitView.getItemCount()>1)
        _splitView.removeChildWithAnim(codeBuildrPane);
}

/**
 * Initialize UI panel.
 */
protected void initUI()
{
    // Do normal version
    super.initUI();
    
    // Get TextView and start listening for events (KeyEvents, MouseClicked, DragOver/Exit/Drop)
    _textView = getTextView(); _textView._textPane = this;
    _textView.setGrowWidth(true);
    enableEvents(_textView, KeyPressed, KeyReleased, KeyTyped, MousePressed, MouseClicked, DragOver, DragExit,DragDrop);
    
    // Reset TextView font
    float fontSize = PrefsUtils.prefs().getFloat("JavaFontSize", 12); if(fontSize<8) fontSize = 12;
    _textView.setFont(new Font(_textView.getDefaultFont().getName(), fontSize));
    
    // Get TextView.RowHeader and configure
    RowHeader rowHeader = createRowHeader();
    rowHeader.setTextView(_textView); _textView._rowHeader = rowHeader;
    
    // Get ScrollView and add RowHeader
    ScrollView spane = getView("ScrollView", ScrollView.class); spane.setGrowWidth(true);
    HBox hbox = new HBox(); hbox.setFillHeight(true);
    hbox.setChildren(rowHeader, _textView);
    spane.setContent(hbox);
    
    // Get SplitView and add ScrollView and CodeBuilder
    _splitView = new SplitView();
    _splitView.addChild(spane);
    getUI(BorderView.class).setCenter(_splitView);
    
    // Get OverviewPane and set JavaTextView
    OverviewPane overviewPane = createOverviewPane();
    overviewPane.setTextView(_textView); _textView._overviewPane = overviewPane;
    getUI(BorderView.class).setRight(overviewPane);
}

/**
 * Reset UI.
 */
public void resetUI()
{    
    // Reset FontSizeText
    setViewValue("FontSizeText", getTextView().getFont().getSize());
    
    // Clear path box and add Lin/Col postion label
    HBox nodePathBox = getView("BottomBox", HBox.class);
    while(nodePathBox.getChildCount()>1) nodePathBox.removeChild(1); Font font = Font.get("Arial",11);
    
    // Iterate up from DeepPart and add parts
    for(JNode part=getTextView()._deepNode, spart=getTextView().getSelectedNode(); part!=null; part=part.getParent()) {
        Label label = new Label(); label.setText(part.getNodeString()); label.setFont(font);
        label.setName("NodePathLabel"); label.setProp("JNode", part);
        if(part==spart) label.setFill(Color.LIGHTGRAY);
        nodePathBox.addChild(label,1); label.setOwner(this); enableEvents(label, MouseClicked);
        Label div = new Label(); div.setText(" \u2022 "); div.setFont(font); if(part.getParent()==null) break;
        nodePathBox.addChild(div,1);
    }
}

/**
 * Get compile info.
 */
public String getSelectionInfo()  { return super.getSelectionInfo() + ": "; }

/**
 * Respond to UI controls.
 */
public void respondUI(ViewEvent anEvent)
{
    // Do normal version
    super.respondUI(anEvent);
    
    // Handle TextView key events
    if(anEvent.equals("TextView")) {
        
        // Handle KeyPressed/KeyReleased to watch for CONTROL/COMMAND press/release
        if(anEvent.isKeyPressed() || anEvent.isKeyReleased()) { int kc = anEvent.getKeyCode();
            if(kc==KeyCode.COMMAND || kc==KeyCode.CONTROL)
                setTextViewHoverEnabled(anEvent.isKeyPressed());
        }
        
        // Handle KeyTyped: If PopupList not visible, ActivatePopupList
        else if(anEvent.isKeyTyped()) { char ch = anEvent.getKeyChar();
            if(getPopup().isShowing() || anEvent.isShortcutDown()) return;
            if(ch==KeyCode.CHAR_UNDEFINED) return;
            if(Character.isISOControl(ch)) return;
            runLater(() -> getTextView().activatePopupList());
        }
        
        // Handle PopupTrigger
        else if(anEvent.isPopupTrigger()) { //anEvent.consume();
            Menu cmenu = createContextMenu(); cmenu.show(_textView, anEvent.getX(), anEvent.getY()); }
        
        // Handle MouseClicked: If alt-down, open JavaDoc. If HoverNode, open declaration
        else if(anEvent.isMouseClicked()) {
            
            // If alt is down and there is a JavaDoc URL, open it
            if(anEvent.isAltDown() && getJavaDocURL()!=null)
                URLUtils.openURL(getJavaDocURL());
                
            // If there is a hover node, open it (and clear Hover)
            else if(getTextView().getHoverNode()!=null) {
                openDeclaration(getTextView().getHoverNode());
                setTextViewHoverEnabled(false);
            }
        }
        
        // Handle MouseMoved
        else if(anEvent.isMouseMoved()) {
            if(!anEvent.isShortcutDown()) { setTextViewHoverEnabled(false); return; }
            int index = _textView.getCharIndex(anEvent.getX(), anEvent.getY());
            JNode node = _textView.getJFile().getNodeAtCharIndex(index);
            _textView.setHoverNode(node instanceof JExprId || node instanceof JType? node : null);
        }
        
        // Handle DragOver, DragExit, DragDrop
        else if(anEvent.isDragOver()) getCodeBuilder().dragOver(anEvent.getX(), anEvent.getY());
        else if(anEvent.isDragExit()) getCodeBuilder().dragExit();
        else if(anEvent.isDragDropEvent()) getCodeBuilder().drop(0, 0);
    }

    // Handle JavaDocButton
    else if(anEvent.equals("JavaDocButton")) URLUtils.openURL(getJavaDocURL());
    
    // Handle CodeBuilderButton
    else if(anEvent.equals("CodeBuilderButton")) setCodeBuilderVisible(!isCodeBuilderVisible());

    // Handle FontSizeText, IncreaseFontButton, DecreaseFontButton
    else if(anEvent.equals("FontSizeText")||anEvent.equals("IncreaseFontButton")||anEvent.equals("DecreaseFontButton"))
        PrefsUtils.prefsPut("JavaFontSize", getTextView().getFont().getSize()); 
    
    // Handle OpenDeclarationMenuItem
    else if(anEvent.equals("OpenDeclarationMenuItem"))
        openDeclaration(getTextView().getSelectedNode());
    
    // Handle ShowReferencesMenuItem
    else if(anEvent.equals("ShowReferencesMenuItem"))
        showReferences(getTextView().getSelectedNode());
    
    // Handle ShowDeclarationsMenuItem
    else if(anEvent.equals("ShowDeclarationsMenuItem"))
        showDeclarations(getTextView().getSelectedNode());
    
    // Handle NodePathLabel
    else if(anEvent.equals("NodePathLabel")) {
        JNode part = (JNode)anEvent.getView().getProp("JNode"), dnode = getTextView()._deepNode;
        getTextView().setSel(part.getStart(), part.getEnd()); getTextView()._deepNode = dnode;
    }
}

/**
 * Save file.
 */
public void saveChanges()
{
    getPopup().hide(); // Close popup
    super.saveChanges();
    getTextView().getTextBox()._jfile = null;
    getTextView().getTextBox().getJFile(); // Force reparse
}

/**
 * Returns the string for the JavaDocButton. Called from binding set up in rib file.
 */
public String getJavaDocText()
{
    Class cls = getTextView().getSelectedNodeClass();
    String className = cls!=null? cls.getSimpleName() : null;
    return className + " Doc";
}

/**
 * Returns the JavaDoc url for currently selected type.
 */
public String getJavaDocURL()
{
    // Get class name for selected JNode
    Class cls = getTextView().getSelectedNodeClass(); if(cls==null) return null;
    String className = cls.getName();
    
    // Handle reportmill class
    String url = null;
    if(className.startsWith("snap"))
        url = "http://reportmill.com/snap1/javadoc/index.html?" + className.replace('.', '/') + ".html";
    else if(className.startsWith("com.reportmill"))
        url = "http://reportmill.com/rm14/javadoc/index.html?" + className.replace('.', '/') + ".html";
    
    // Handle standard java classes
    else if(className.startsWith("java.") || className.startsWith("javax."))
        url = "http://docs.oracle.com/javase/8/docs/api/index.html?" + className.replace('.', '/') + ".html";
    
    // Handle JavaFX classes
    else if(className.startsWith("javafx."))
        url = "http://docs.oracle.com/javafx/2/api/index.html?" + className.replace('.', '/') + ".html";
    
    // Return url
    return url;
}

/**
 * Sets whether MouseMoved over JavaTextView should set hover node.
 */
protected void setTextViewHoverEnabled(boolean isEnabled)
{
    if(isEnabled) enableEvents(_textView, MouseMoved);
    else disableEvents(_textView, MouseMoved);
    _textView.setHoverNode(null);
}

/**
 * Override to turn off TextViewHoverEnabled.
 */
public void showLineNumberPanel()  { super.showLineNumberPanel(); setTextViewHoverEnabled(false); }

/**
 * Creates the ContextMenu.
 */
protected Menu createContextMenu()
{
    Menu cm = new Menu(); //cm.setAutoHide(true); cm.setConsumeAutoHidingEvents(true);
    MenuItem mi1 = new MenuItem(); mi1.setText("Open Declaration"); mi1.setName("OpenDeclarationMenuItem");
    MenuItem mi2 = new MenuItem(); mi2.setText("Show References"); mi2.setName("ShowReferencesMenuItem");
    MenuItem mi3 = new MenuItem(); mi3.setText("Show Declarations"); mi3.setName("ShowDeclarationsMenuItem");
    cm.addItem(mi1); cm.addItem(mi2); cm.addItem(mi3); cm.setOwner(this);
    return cm;
}

/**
 * Creates the RowHeader.
 */
protected RowHeader createRowHeader()  { return new RowHeader(); }

/**
 * Creates the OverviewPane.
 */
protected OverviewPane createOverviewPane()  { return new OverviewPane(); }

/**
 * Sets the TextSelection.
 */
public void setTextSel(int aStart, int anEnd)  { _textView.setSel(aStart, anEnd); }

/**
 * Open declaration.
 */
public void openDeclaration(JNode aNode)  { }

/**
 * Open a super declaration.
 */
public void openSuperDeclaration(JMemberDecl aMemberDecl)  { }

/**
 * Show References.
 */
public void showReferences(JNode aNode)  { }

/**
 * Show declarations.
 */
public void showDeclarations(JNode aNode)  { }

/**
 * Returns the ProgramCounter line.
 */
public int getProgramCounterLine()  { return -1; }

}