package snap.javasnap;
import snap.javatext.JavaPage;
import snap.view.*;
import snap.viewx.*;
import snap.web.WebURL;

/**
 * A WebPage to wrap around SnapEditorPane.
 */
public class SnapEditorPage extends WebPage {

    // The JavaPage
    JavaPage          _javaPage;

    // The SnapEditorPane
    SnapEditorPane    _editorPane;
    
/**
 * Create new SnapEditorPage.
 */
public SnapEditorPage(JavaPage aJavaPage)
{
    _javaPage = aJavaPage;
    _editorPane = new SnapEditorPane(aJavaPage.getTextPane()); _editorPane._snapPage = this;
}

/**
 * Create UI.
 */
protected View createUI()  { return _editorPane.getUI(); }

/**
 * Reopen this page as SnapCodePage.
 */
public void openAsJavaText()
{
    WebURL url = getURL();
    WebBrowser browser = getBrowser(); browser.setPage(url, _javaPage);
    browser.setURL(url);
}

}