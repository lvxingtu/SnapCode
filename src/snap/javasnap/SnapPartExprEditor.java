package snap.javasnap;
import snap.javaparse.*;
import snap.javasnap.SnapPartPane.Seg;
import snap.view.*;

/**
 * A SnapPartExpr subclass to do text editing on expression.
 */
public class SnapPartExprEditor <JNODE extends JExpr>  extends SnapPartExpr<JNODE> {

    // The text field
    TextField    _tfield;

    // The text field name
    static final String TextFieldName = "ExprText";

/**
 * Creates UI.
 */
public View createUI()
{
    SnapPartPane superUI = (SnapPartPane)super.createUI();
    superUI.setSeg(Seg.Middle); superUI.setColor(null);
    return superUI;
}

/**
 * Creates UI.
 */
protected void configureHBox(HBox aHBox)
{
    // Get expression
    JExpr expr = getJNode(); String str = expr.getString();
    
    // Create text field, configure and return
    _tfield = createTextField(str); _tfield.setName(TextFieldName); _tfield.setPrefHeight(18);
    enableEvents(_tfield, KeyReleased); //enableEvents(_tfield, DragEvents);
    aHBox.addChild(_tfield);
}

/** Fires TextFieldAction. */
void fireTextFieldAction()  { _tfield.fireActionEvent(); }

/**
 * Responds to UI.
 */
protected void respondUI(ViewEvent anEvent)
{
    // Handle KeyEvents
    if(anEvent.isKeyReleased())
        runLater(() -> handleKeyFinished(anEvent));
    
    // Handle ExprText
    else if(anEvent.equals(TextFieldName)) handleCodeCompletion();
    
    // Handle normal
    else super.respondUI(anEvent);
}

/**
 * Handle KeyEvents.
 */
protected void handleKeyFinished(ViewEvent anEvent)
{
    // Handle KeyFinished: Update PopupList and reset TextField with PrefWidth
    SnapEditorPopup.getShared().activatePopupList(this, _tfield.getText(), _tfield.getSelStart());
}

/**
 * Called to insert CodeComp into expression.
 */
protected void handleCodeCompletion()
{
    SnapEditorPopup hpop = SnapEditorPopup.getShared();
    String str = getViewStringValue(_tfield); if(hpop.isShowing()) str = hpop.getFixedText();
    getCodeArea().replaceJNode(getJNode(), str);
    hpop.hide();
}

/**
 * Drops a node.
 */
protected void dropNode(JNode aJNode, double anX, double aY)
{
    if(aJNode instanceof JStmtExpr)
        aJNode = ((JStmtExpr)aJNode).getExpr();
    if(!(aJNode instanceof JExpr)) {
        System.out.println("SnapPartExprEditor: Can't drop node " + aJNode); return; }
    
    // Replace expression with DropNode
    String str = aJNode.getString();
    getCodeArea().replaceJNode(getJNode(), str);
}
    
}