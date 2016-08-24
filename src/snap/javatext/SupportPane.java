package snap.javatext;
import snap.gfx.*;
import snap.javaparse.*;
import snap.parse.*;
import snap.view.*;

/**
 * UI to show puzzle pieces.
 */
public class SupportPane extends ViewOwner {

    // The SnapEditorPane
    SnapEditorPane         _editorPane;
    
    // The SnapPart being dragged
    static SnapPart        _dragSP;
    
    // The drag image
    static Image           _dragImage;
    
    // The statement parser and expression parser
    Parser             _stmtParser = JavaParser.getShared().getStmtParser();
    Parser             _exprParser = JavaParser.getShared().getExprParser();

/**
 * Returns the editor.
 */
public SnapEditorPane getEditorPane()  { return _editorPane; }

/**
 * Create UI.
 */
protected View createUI()
{
    TabView tpane = new TabView();
    tpane.addTab("Methods", createMethodsPane());
    tpane.addTab("Blocks", createBlocksPane());
    return tpane;
}

/**
 * Configure UI.
 */
protected void initUI()
{
    // Add DragDetected action to start statement drag
    /*getDragUI().setOnDragDetected(e -> {
        _dragSP = getSnapPart(getDragUI(), e.getX(), e.getY()); if(_dragSP==null) return;
        javafx.scene.SnapshotParameters sp = new javafx.scene.SnapshotParameters(); sp.setFill(Color.TRANSPARENT);
        _dragImage = _dragSP.getNative().snapshot(sp, null);
        //JNode copy = _stmtParser.parseCustom(_dragSP.getJNode().getString(), JNode.class);
        //_dragSP = SnapPart.createSnapPart(copy);
        //Dragboard db = getDragUI().startDragAndDrop(TransferMode.ANY);
        //ClipboardContent cc = new ClipboardContent(); cc.putString("Hello World"); db.setContent(cc);
        //e.consume(); db.setDragView(_dragImage);
    });*/
    
    enableEvents(getUI(), MouseClicked, DragGesture);
}

/**
 * Respond to UI.
 */
protected void respondUI(ViewEvent anEvent)
{
    // Handle MouseClicked (double click)
    if(anEvent.isMouseClicked() && anEvent.getClickCount()==2) {
        SnapPart part = getSnapPart(getUI(ParentView.class), anEvent.getX(), anEvent.getY());
        if(part!=null)
            getEditorPane().getSelectedPart().dropNode(part.getJNode());
    }
    
    // Handle Drag: Get Dragboard and drag shape with image (with DragSourceListener to clear DragShape)
    if(anEvent.isDragGesture()) {
        
        // Get drag node
        _dragSP = getSnapPart(getUI(ParentView.class), anEvent.getX(), anEvent.getY()); if(_dragSP==null) return;
        
        // Create Dragboard, set image and start drag
        Dragboard dboard = anEvent.getDragboard();
        dboard.setContent("SupportPane:" + _dragSP.getClass().getSimpleName());
        Image img = ViewUtils.getImage(_dragSP.getUI()); dboard.setDragImage(img);
        dboard.startDrag();
    }
}

/**
 * Update tab.
 */
public void rebuildUI()
{
    // Get class for SnapPart.JNode
    Class cls = getEditorPane().getSelectedPartEnclClass();
    
    ScrollView spane = (ScrollView)getUI(TabView.class).getTabContent(0);
    VBox pane = (VBox)spane.getContent();
    pane.removeChildren();
    
    // Add pieces for classes
    for(Class c=cls; c!=null; c=c.getSuperclass()) updateTabView(c, pane);
}

public void updateTabView(Class aClass, ChildView aPane)
{
    String strings[] = null; String sname = aClass.getSimpleName();
    if(sname.equals("SnapActor")) strings = SnapActorPieces;
    else if(sname.equals("SnapPen")) strings = SnapPenPieces;
    else if(sname.equals("SnapScene")) strings = SnapScenePieces;
    //else try { strings = (String[])ClassUtils.getMethod(aClass, "getSnapPieces").invoke(null); } catch(Exception e){ }
    if(strings==null) return;
    
    for(String str : strings) {
        SnapPart move = createSnapPartStmt(str);
        aPane.addChild(move.getUI());
    }
}

/**
 * Returns the motion pane.
 */
private View createMethodsPane()
{
    // Create vertical box
    VBox pane = new VBox(); pane.setPadding(20,20,20,20); pane.setSpacing(16);
    pane.setGrowWidth(true); pane.setGrowHeight(true); pane.setFill(Color.GRAY); //pane.setBorder(bevel);
    
    // Wrap in ScrollView and return
    ScrollView spane = new ScrollView(pane); spane.setPrefWidth(200);
    return spane;
}

/**
 * Returns the control pane.
 */
private View createBlocksPane()
{
    VBox pane = new VBox(); pane.setPadding(20,20,20,20); pane.setSpacing(16);
    pane.setGrowWidth(true); pane.setGrowHeight(true); pane.setFill(Color.GRAY); //pane.setBorder(bevel);
    
    // Add node for while(true)
    SnapPart ws = createSnapPartStmt("while(true) {\n}");
    pane.addChild(ws.getUI());
    
    // Add node for repeat(x)
    SnapPart fs = createSnapPartStmt("for(int i=0; i<10; i++) {\n}");
    pane.addChild(fs.getUI());
    
    // Add node for if(expr)
    SnapPart is =  createSnapPartStmt("if(true) {\n}");
    pane.addChild(is.getUI());

    // Wrap in ScrollView and return
    ScrollView spane = new ScrollView(pane); spane.setPrefWidth(200);
    return spane;
}

/**
 * Returns a SnapPart for given string of code.
 */
protected SnapPart createSnapPartStmt(String aString)
{
    JNode node = _stmtParser.parseCustom(aString, JNode.class);
    return SnapPart.createSnapPart(node);
}

/**
 * Returns the child of given class hit by coords.
 */
protected SnapPart getSnapPart(ParentView aPar, double anX, double aY)
{
    for(View child : aPar.getChildren()) {
        if(!child.isVisible()) continue;
        Point p = child.parentToLocal(anX, aY);
        if(child.contains(p.getX(), p.getY()) && SnapPart.getSnapPart(child)!=null)
            return SnapPart.getSnapPart(child);
        if(child instanceof ParentView) { ParentView par = (ParentView)child;
            SnapPart no = getSnapPart(par, p.getX(), p.getY());
            if(no!=null)
                return no; }
    }
    return null;
}

/** Returns SnapActor pieces. */
private static String SnapActorPieces[] = { "moveBy(10);", "turnBy(10);", "scaleBy(.1);",
    "getX();", "getY();", "getWidth();", "getHeight();", "setXY(10,10);", "setSize(50,50);",
    "getRotate();", "setRotate(10);", "getScale();", "setScale(1);",
    "getAngle(\"Mouse\");", "getDistance(\"Mouse\");", "isMouseDown();", "isMouseClicked();",
    "isKeyDown(\"right\");", "isKeyClicked(\"right\");", "playSound(\"Beep.wav\");", "getScene();",
    "getPen();", "setPenColor(\"Random\");", "penDown();", "getAnimator();" };

/** Returns SnapPen pieces. */
private static String SnapPenPieces[] = { "down();", "up();", "clear();", "setColor(\"Random\");", "setWidth(10);" };

/** Returns SnapScene pieces. */
private static String SnapScenePieces[] = { "getWidth();", "getHeight();",
    "isMouseDown();", "isMouseClicked();", "getMouseX();", "getMouseY();", "isKeyDown(\"right\");",
    "isKeyClicked(\"right\");", "getActor(\"Cat1\");", "playSound(\"Beep.wav\");",
    "setColor(\"Random\");", "setShowCoords(true);" };

}