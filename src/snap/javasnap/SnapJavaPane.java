package snap.javasnap;
import snap.gfx.*;
import snap.javaparse.*;
import snap.javatext.JavaTextBox;
import snap.javatext.JavaTextPane;
import snap.javatext.JavaTextView;
import snap.view.ViewEvent;

/**
 * A class to handle code editing for SnapEditor.
 */
public class SnapJavaPane extends JavaTextPane {

    // The SnapEditorPane
    SnapEditorPane      _editorPane;

    // The SnapCodePane
    SnapCodePane        _codePane;

/**
 * Replaces a string.
 */
protected void replaceText(String aString, int aStart, int anEnd)
{
    JavaTextView tarea = getTextView();
    tarea.undoerSaveChanges();
    tarea.replaceChars(aString, null, aStart, anEnd, true);
}

/**
 * Sets text selection.
 */
protected void setTextSelection(int aStart, int anEnd)
{
    JavaTextView tarea = getTextView();
    tarea.setSel(aStart, anEnd);
}

/**
 * Insets a node.
 */
public void insertNode(JNode aBaseNode, JNode aNewNode, int aPos)
{
    if(aBaseNode instanceof JFile) { System.out.println("Can't add to file"); return; }
    
    if(aBaseNode instanceof JStmtExpr && aNewNode instanceof JStmtExpr &&
        aBaseNode.getJClass()==_editorPane.getSelectedPartClass() && aBaseNode.getJClass()!=void.class) {
        int index = aBaseNode.getEnd();
        String nodeStr = aNewNode.getString(), str = '.' + nodeStr;
        replaceText(str, index - 1, index);
        setTextSelection(index, index + nodeStr.length());
    }
    
    else {
        int index = aPos<0? getBeforeNode(aBaseNode) : aPos>0? getAfterNode(aBaseNode) : getInNode(aBaseNode);
        String indent = getIndent(aBaseNode, aPos);
        String nodeStr = aNewNode.getString().replace("\n", "\n" + indent);
        String str = indent + nodeStr;
        replaceText(str + '\n', index, index);
        setTextSelection(index + indent.length(), index + indent.length() + nodeStr.trim().length());
    }
}

/**
 * Replaces a JNode with string.
 */
public void replaceJNode(JNode aNode, String aString)
{
    replaceText(aString, aNode.getStart(), aNode.getEnd());
}

/**
 * Removes a node.
 */
public void removeNode(JNode aNode)
{
    int start = getBeforeNode(aNode), end = getAfterNode(aNode);
    replaceText(null, start, end);
}

/**
 * Returns after node.
 */
public int getBeforeNode(JNode aNode)
{
    int index = aNode.getStart();
    JExpr pexpr = aNode instanceof JExpr? ((JExpr)aNode).getParentExpr() : null; if(pexpr!=null) return pexpr.getEnd();
    TextBoxLine tline = getTextView().getLineAt(index);
    return tline.getStart();
}

/**
 * Returns after node.
 */
public int getAfterNode(JNode aNode)
{
    int index = aNode.getEnd();
    JExprChain cexpr = aNode.getParent() instanceof JExprChain? (JExprChain)aNode.getParent() : null;
    if(cexpr!=null) return cexpr.getExpr(cexpr.getExprCount()-1).getEnd();
    TextBoxLine tline = getTextView().getLineAt(index);
    return tline.getEnd();
}

/**
 * Returns in the node.
 */
public int getInNode(JNode aNode)
{
    JavaTextView tarea = getTextView();
    int index = aNode.getStart(); while(index<tarea.length() && tarea.charAt(index)!='{') index++;
    TextBoxLine tline = getTextView().getLineAt(index);
    return tline.getEnd();
}

/**
 * Returns the indent.
 */
String getIndent(JNode aNode, int aPos)
{
    int index = aNode.getStart();
    TextBoxLine tline = getTextView().getLineAt(index);
    int c = 0; while(c<tline.length() && Character.isWhitespace(tline.charAt(c))) c++;
    StringBuffer sb = new StringBuffer(); for(int i=0;i<c;i++) sb.append(' ');
    if(aPos==0) sb.append("    ");
    return sb.toString();
}

/**
 * Respond to events.
 */
public void respondUI(ViewEvent anEvent)
{
    if(anEvent.equals("SnapCodeButton")) {
        _codePane.setShowSnapCode(true); _editorPane.rebuildLater(); }
    else super.respondUI(anEvent);
}

/**
 * Override to create custom JavaTextView.
 */
@Override
protected SnapJavaTextView createTextView()  { return new SnapJavaTextView(); }

/**
 * JavaTextView subclass to create custom JavaText.
 */
protected class SnapJavaTextView extends JavaTextView {

    /** Create Text. */
    @Override
    protected SnapJavaText createText()  { return new SnapJavaText(); }
}

/**
 * JavaText subclass to rebuild SnapCodePane on addChars/removeChars.
 */
protected class SnapJavaText extends JavaTextBox {

    /** Override to adjust build issues start/end. */
    @Override
    public void updateLines(int aStart, int endOld, int endNew)
    {
        super.updateLines(aStart, endOld, endNew);
        if(_codePane.getShowSnapCode()) _editorPane.rebuildLater();
    }
}

}