package snap.javasnap;
import snap.javakit.*;
import snap.view.*;

/**
 * SnapPartStmt subclass for JStmtFor.
 */
public class JStmtForView <JNODE extends JStmtFor> extends JStmtView <JNODE> {

/**
 * Updates UI for top line.
 */
protected void updateHBox(HBox theHBox)
{
    JStmtFor fs = getJNode();
    Label label = createLabel("for");
    theHBox.addChild(label);
    
    // Add init declaration text
    if(fs.getInitDecl()!=null) { JStmtVarDecl ivd = fs.getInitDecl(); String str = ivd.getString();
        TextField tfield = createTextField(str); tfield.setName("ExprText"); tfield.setProp("Expr", ivd);
        tfield.addEventHandler(e -> handleTextEvent(e));
        theHBox.addChild(tfield);
    }
    
    // Add conditional text
    if(fs.getConditional()!=null) { JExpr cond = fs.getConditional(); String str = cond.getString();
        TextField tfield = createTextField(str); tfield.setName("ExprText"); tfield.setProp("Expr", cond);
        tfield.addEventHandler(e -> handleTextEvent(e));
        theHBox.addChild(tfield);
    }
    
    // Add update statement text
    if(fs.getUpdateStmts()!=null && fs.getUpdateStmts().size()>0) {
        JStmtExpr se = fs.getUpdateStmts().get(0); String str = se.getString();
        TextField tfield = createTextField(str); tfield.setName("ExprText"); tfield.setProp("Expr", se);
        tfield.addEventHandler(e -> handleTextEvent(e));
        theHBox.addChild(tfield);
    }
}

/**
 * Responds to UI.
 */
protected void handleTextEvent(ViewEvent anEvent)
{
    TextField tfield = anEvent.getView(TextField.class);
    JNode jnode = (JNode)tfield.getProp("Expr");
    getCodeArea().replaceJNode(jnode, anEvent.getStringValue());
}

}