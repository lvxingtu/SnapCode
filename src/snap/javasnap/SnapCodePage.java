package snap.javasnap;
import snap.javatext.JavaPage;
import snap.view.*;
import snap.viewx.WebPage;

/**
 * A custom class.
 */
public class SnapCodePage extends WebPage {

    // The JavaPage
    JavaPage          _javaPage;

    // The SnapEditorPane
    SnapEditorPane    _editorPane;
    
/**
 * Create new SnapCodePage.
 */
public SnapCodePage(JavaPage aJavaPage)
{
    _javaPage = aJavaPage;
    _editorPane = new SnapEditorPane(aJavaPage.getTextPane());
}

/**
 * Create UI.
 */
protected View createUI()  { return _editorPane.getUI(); }

}