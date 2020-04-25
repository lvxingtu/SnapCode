package javakit.views;
import javakit.parse.JNode;
import javakit.parse.JavaParser;
import snap.geom.*;
import snap.gfx.*;
import snap.parse.*;
import snap.view.*;

/**
 * UI to show puzzle pieces.
 */
public class SupportPane extends ViewOwner {

    // The SnapEditorPane
    SnapEditorPane         _editorPane;
    
    // The SnapPart being dragged
    static JNodeView        _dragSP;
    
    // The drag image
    static Image           _dragImage;
    
    // The statement parser and expression parser
    Parser             _stmtParser = JavaParser.getShared().getStmtParser();
    Parser             _exprParser = JavaParser.getShared().getExprParser();
    
    /**
     * Returns the editor pane.
     */
    public SnapEditorPane getEditorPane()  { return _editorPane; }

    /**
     * Returns the editor.
     */
    public SnapEditor getEditor()  { return _editorPane.getEditor(); }

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

        enableEvents(getUI(), MouseRelease, DragGesture);
    }

    /**
     * Respond to UI.
     */
    protected void respondUI(ViewEvent anEvent)
    {
        // Handle MouseClick (double click)
        if(anEvent.isMouseClick() && anEvent.getClickCount()==2) {
            JNodeView part = getSnapPart(getUI(ParentView.class), anEvent.getX(), anEvent.getY());
            if(part!=null)
                getEditorPane().getSelectedPart().dropNode(part.getJNode());
        }

        // Handle Drag: Get Dragboard and drag shape with image (with DragSourceListener to clear DragShape)
        if(anEvent.isDragGesture()) {

            // Get drag node
            _dragSP = getSnapPart(getUI(ParentView.class), anEvent.getX(), anEvent.getY()); if(_dragSP==null) return;

            // Create Dragboard, set image and start drag
            Clipboard cboard = anEvent.getClipboard();
            cboard.addData("SupportPane:" + _dragSP.getClass().getSimpleName());
            Image img = ViewUtils.getImage(_dragSP); cboard.setDragImage(img);
            cboard.startDrag();
        }
    }

    /**
     * Update tab.
     */
    public void rebuildUI()
    {
        // Get class for SnapPart.JNode
        Class cls = getEditor().getSelectedPartEnclClass();

        ScrollView spane = (ScrollView)getUI(TabView.class).getTabContent(0);
        ColView pane = (ColView)spane.getContent();
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
            JNodeView move = createSnapPartStmt(str);
            aPane.addChild(move);
        }
    }

    /**
     * Returns the motion pane.
     */
    private View createMethodsPane()
    {
        // Create vertical box
        ColView pane = new ColView(); pane.setPadding(20,20,20,20); pane.setSpacing(16);
        pane.setGrowWidth(true); pane.setGrowHeight(true); pane.setFill(JFileView.BACK_FILL); //pane.setBorder(bevel);

        // Wrap in ScrollView and return
        ScrollView spane = new ScrollView(pane); spane.setPrefWidth(200);
        return spane;
    }

    /**
     * Returns the control pane.
     */
    private View createBlocksPane()
    {
        ColView pane = new ColView(); pane.setPadding(20,20,20,20); pane.setSpacing(16);
        pane.setGrowWidth(true); pane.setGrowHeight(true); pane.setFill(JFileView.BACK_FILL); //pane.setBorder(bevel);

        // Add node for while(true)
        JNodeView ws = createSnapPartStmt("while(true) {\n}");
        pane.addChild(ws);

        // Add node for repeat(x)
        JNodeView fs = createSnapPartStmt("for(int i=0; i<10; i++) {\n}");
        pane.addChild(fs);

        // Add node for if(expr)
        JNodeView is =  createSnapPartStmt("if(true) {\n}");
        pane.addChild(is);

        // Wrap in ScrollView and return
        ScrollView spane = new ScrollView(pane); spane.setPrefWidth(200);
        return spane;
    }

    /**
     * Returns a SnapPart for given string of code.
     */
    protected JNodeView createSnapPartStmt(String aString)
    {
        JNode node = _stmtParser.parseCustom(aString, JNode.class);
        JNodeView nview = JNodeView.createView(node);
        nview.getEventAdapter().disableEvents(DragEvents);
        return nview;
    }

    /**
     * Returns the child of given class hit by coords.
     */
    protected JNodeView getSnapPart(ParentView aPar, double anX, double aY)
    {
        for(View child : aPar.getChildren()) {
            if(!child.isVisible()) continue;
            Point p = child.parentToLocal(anX, aY);
            if(child.contains(p.getX(), p.getY()) && JNodeView.getJNodeView(child)!=null)
                return JNodeView.getJNodeView(child);
            if(child instanceof ParentView) { ParentView par = (ParentView)child;
                JNodeView no = getSnapPart(par, p.getX(), p.getY());
                if(no!=null)
                    return no; }
        }
        return null;
    }

    /** Returns SnapActor pieces. */
    private static String SnapActorPieces[] = { "moveBy(10);", "turnBy(10);", "scaleBy(.1);",
        "getX();", "getY();", "getWidth();", "getHeight();", "setXY(10,10);", "setSize(50,50);",
        "getRotate();", "setRotate(10);", "getScale();", "setScale(1);",
        "getAngle(\"Mouse\");", "getDistance(\"Mouse\");", "isMouseDown();", "isMouseClick();",
        "isKeyDown(\"right\");", "isKeyClicked(\"right\");", "playSound(\"Beep.wav\");", "getScene();",
        "getPen();", "setPenColor(\"Random\");", "penDown();", "getAnimator();" };

    /** Returns SnapPen pieces. */
    private static String SnapPenPieces[] = { "down();", "up();", "clear();", "setColor(\"Random\");", "setWidth(10);" };

    /** Returns SnapScene pieces. */
    private static String SnapScenePieces[] = { "getWidth();", "getHeight();",
        "isMouseDown();", "isMouseClick();", "getMouseX();", "getMouseY();", "isKeyDown(\"right\");",
        "isKeyClicked(\"right\");", "getActor(\"Cat1\");", "playSound(\"Beep.wav\");",
        "setColor(\"Random\");", "setShowCoords(true);" };

}