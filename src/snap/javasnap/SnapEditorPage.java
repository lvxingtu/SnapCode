package snap.javasnap;
import snap.javatext.JavaPage;
import snap.view.*;
import snap.viewx.*;
import snap.web.*;

/**
 * A WebPage to wrap around SnapEditorPane.
 */
public class SnapEditorPage extends WebPage {

    // The JavaPage
    JavaPage          _javaPage;

    // The SnapEditorPane
    SnapEditorPane    _editorPane;
    
/**
 * Creates a SnapEditorPage.
 */
public SnapEditorPage()
{
    _javaPage = new JavaPage();
    _editorPane = new SnapEditorPane(_javaPage.getTextPane()); _editorPane._snapPage = this;
}

/**
 * Creates a SnapEditorPage.
 */
public SnapEditorPage(JavaPage aJavaPage)
{
    _javaPage = aJavaPage;
    _editorPane = new SnapEditorPane(aJavaPage.getTextPane()); _editorPane._snapPage = this;
}

/** Override to forward to JavaPage. */
public void setBrowser(WebBrowser aBrowser)  { super.setBrowser(aBrowser); _javaPage.setBrowser(aBrowser); }

/** Override to forward to JavaPage. */
public void setURL(WebURL aURL)  { super.setURL(aURL); _javaPage.setURL(aURL); }

/** Override to forward to JavaPage. */
public void setFile(WebFile aFile)  { super.setFile(aFile); _javaPage.setFile(aFile); }

/** Override to forward to JavaPage. */
public void setResponse(WebResponse aResp)  { super.setResponse(aResp); _javaPage.setResponse(aResp); }

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

/**
 * Returns whether given file wants to be SanpEditorPage.
 */
public static boolean isSnapEditSet(WebFile aFile)
{
    //JavaData jdata = JavaData.get(aFile); Class cls = jdata.getJFile().getEvalClass();
    //for(Class c=cls;c!=null;c=c.getSuperclass()) if(c.getSimpleName().equals("SnapActor")) return true;
    
    // Return true if 'SnapEdit=true' is found in the first comment
    String str = aFile.getText(); if(str==null) return false;
    str = str.substring(0, str.indexOf("*/")+1); 
    return str.contains("SnapEdit=true");
}

}