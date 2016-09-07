package snap.app;
import snap.javaparse.*;
import snap.javatext.SnapJavaPane;
import snap.project.Project;
import snap.viewx.*;
import snap.web.*;

/**
 * A JavaTextPane for a Snap JavaFile.
 */
public class JavaPageJavaPane extends SnapJavaPane implements WebFile.Updater {

    // The BrowserPage
    WebPage        _page;

/**
 * Creates a JavaPageTextPane for given JavaFilePage.
 */
public JavaPageJavaPane(WebPage aPage)  { _page = aPage; }

/**
 * Returns the BrowserPage.
 */
public WebPage getPage()  { return _page; }

/**
 * Return the WebPage Browser.
 */
public WebBrowser getBrowser()  { return _page.getBrowser(); }

/**
 * Return the AppPane.
 */
AppPane getAppPane()  { return getBrowser() instanceof AppBrowser? ((AppBrowser)getBrowser()).getAppPane() : null; }

/**
 * Return JavaFile for Text.
 */
public WebFile getFile()  { return getTextView().getTextBox().getSourceFile(); }

/**
 * Return site for JavaFile.
 */
public WebSite getSite()  { return getFile().getSite(); }

/**
 * Override to set TextView.File from page.
 */
protected void initUI()
{
    super.initUI();
    getTextView().setSource(_page.getFile());
}

/**
 * Override to set selection using browser.
 */
public void setTextSel(int aStart, int anEnd)
{
    String urls = getFile().getURL().getString() + String.format("#Sel=%d-%d", aStart, anEnd);
    getBrowser().setURLString(urls);
}

/**
 * Override to open declaration.
 */
public void openDeclaration(JNode aNode)
{
    JavaDecl decl = aNode.getDecl();
    if(decl!=null) openDecl(decl);
}

/**
 * Open a super declaration.
 */
public void openSuperDeclaration(JMemberDecl aMemberDecl)
{
    JavaDecl sdecl = aMemberDecl.getSuperDecl(); if(sdecl==null) return;
    openDecl(sdecl);
}

/**
 * Opens a project declaration.
 */
public void openDecl(JavaDecl aDecl)
{
    // Get class name (if Java class, open Java source)
    String cname = aDecl.getClassName(); if(cname==null) return;
    if(cname.startsWith("java.") || cname.startsWith("javax.") || cname.startsWith("javafx.")) {
        openJavaDecl(aDecl); return; }

    // Open decl in project file
    Project proj = Project.get(getSite()); if(proj==null) return;
    WebFile file = proj.getProjectSet().getJavaFile(cname); if(file==null) return;
    JavaData jdata = JavaData.get(file);
    JNode node = JavaDecl.getDeclMatch(proj, jdata.getJFile(), aDecl);
    String urls = file.getURL().getString();
    if(node!=null) urls += String.format("#Sel=%d-%d", node.getStart(), node.getEnd());
    getBrowser().setURLString(urls);
}

/**
 * Override to open declaration.
 */
public void openJavaDecl(JavaDecl aDecl)
{
    String cname = aDecl.getClassName(); if(cname==null) return;
    String cpath = '/' + cname.replace('.', '/') + ".java";
    WebURL jurl = WebURL.getURL("http://reportmill.com/jars/8u05/src.zip!" + cpath);
    if(cname.startsWith("javafx.")) jurl = WebURL.getURL("http://reportmill.com/jars/8u05/javafx-src.zip!" + cpath);
    String urls = jurl.getString() + "#Member=" + aDecl.getSimpleName();
    getBrowser().setURLString(urls);
}

/**
 * Show references for given node.
 */
public void showReferences(JNode aNode)
{
    if(getAppPane()==null) return;
    getAppPane().getSearchPane().searchReference(aNode);
    getAppPane().setSupportTrayIndex(SupportTray.SEARCH_PANE);
}

/**
 * Show declarations for given node.
 */
public void showDeclarations(JNode aNode)
{
    if(getAppPane()==null) return;
    getAppPane().getSearchPane().searchDeclaration(aNode);
    getAppPane().setSupportTrayIndex(SupportTray.SEARCH_PANE);
}

/**
 * Override to update Page.Modified.
 */
public void setTextModified(boolean aFlag)
{
    super.setTextModified(aFlag);
    getFile().setUpdater(aFlag? this : null);
}

/** WebFile.Updater method. */
public void updateFile(WebFile aFile)  { getFile().setText(getTextView().getText()); }

/** Override to get ProgramCounter from ProcPane. */
@Override
protected int getProgramCounterLine()
{
    if(getAppPane()==null) return -1;
    return getAppPane().getProcPane().getProgramCounter(getFile());
}

}