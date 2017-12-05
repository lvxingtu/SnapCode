package snap.javatext;
import snap.app.AppBrowser;
import snap.app.AppPane;
import snap.app.SupportTray;
import snap.gfx.TextBoxLine;
import snap.javakit.*;
import snap.project.*;
import snap.util.*;
import snap.view.View;
import snap.viewx.*;
import snap.web.*;

/**
 * A JavaPage subclass to view/edit Java files.
 */
public class JavaPage extends WebPage implements WebFile.Updater {

    // The JavaTextPane
    JavaTextPane           _jtextPane = new JPJavaTextPane();

/**
 * Creates a new JavaPage.
 */
public JavaPage()  { _jtextPane._javaPage = this; }

/**
 * Return the AppPane.
 */
AppPane getAppPane()  { return getBrowser() instanceof AppBrowser? ((AppBrowser)getBrowser()).getAppPane() : null; }

/**
 * Returns the JavaTextArea.
 */
public JavaTextPane getTextPane()  { return _jtextPane; }

/**
 * Returns the JavaTextArea.
 */
public JavaTextArea getTextArea()  { return getTextPane().getTextArea(); }

/**
 * Creates UI panel.
 */
protected View createUI()  { return _jtextPane.getUI(); }

/**
 * Init UI.
 */
protected void initUI()
{
    super.initUI();
    getTextArea().setSource(getFile());
    setFirstFocus(getTextArea());
}

/**
 * Override to set parameters.
 */
public void setResponse(WebResponse aResp)
{
    // Do normal version
    super.setResponse(aResp);
    
    // If no real file, just return
    if(getFile()==null)
        return;
    
    // Load UI
    getUI();
    
    // Look for LineNumber
    WebURL aURL = aResp.getRequestURL();
    String lineNumberString = aURL.getRefValue("LineNumber");
    if(lineNumberString!=null) { int lineNumber = SnapUtils.intValue(lineNumberString);
        getTextArea().selectLine(lineNumber-1); }
    
    // Look for Sel (selection)
    String sel = aURL.getRefValue("Sel");
    if(sel!=null) {
        int start = SnapUtils.intValue(sel); sel = sel.substring(sel.indexOf('-')+1);
        int end = SnapUtils.intValue(sel); if(end<start) end = start;
        getTextArea().setSel(start, end);
    }
        
    // Look for SelLine (select line)
    String selLine = aURL.getRefValue("SelLine");
    if(selLine!=null) { int lineNum = SnapUtils.intValue(selLine)-1;
        TextBoxLine tline = lineNum>=0 && lineNum<getTextArea().getLineCount()? getTextArea().getLine(lineNum) : null;
        if(tline!=null) getTextArea().setSel(tline.getStart());
    }
    
    // Look for Find
    String findString = aURL.getRefValue("Find");
    if(findString!=null)
        getTextPane().find(findString, true, true);
    
    // Look for Member selection request
    String memberName = aURL.getRefValue("Member");
    if(memberName!=null) {
        JFile jfile = JavaData.get(getFile()).getJFile();
        JClassDecl cd = jfile.getClassDecl();
        JExprId id = null;
        if(cd.getName().equals(memberName)) id = cd.getId();
        else for(JMemberDecl md : cd.getMemberDecls())
            if(md.getName()!=null && md.getName().equals(memberName)) { id = md.getId(); break; }
        if(id!=null)
            getTextArea().setSel(id.getStart(), id.getEnd());
    }
}

/**
 * Reopen this page as SnapCodePage.
 */
public void openAsSnapCode()
{
    WebFile file = getFile(); WebURL url = file.getURL();
    WebPage page = new snap.javasnap.SnapEditorPage(this); page.setFile(file);
    WebBrowser browser = getBrowser(); browser.setPage(url, page);
    browser.setURL(file.getURL());
}

/**
 * Creates a new file for use with showNewFilePanel method.
 */
protected WebFile createNewFile(String aPath)
{
    // Create file
    WebFile file = super.createNewFile(aPath);
    
    // Create text
    StringBuffer sb = new StringBuffer(); Project proj = Project.get(file);
    String pkgName = proj!=null? proj.getPackageName(file) : file.getSimpleName();
    if(pkgName.length()>0) sb.append("package ").append(pkgName).append(";\n");
    sb.append("\n/**\n * A custom class.\n */\n");
    sb.append("public class ").append(file.getSimpleName()).append(" extends Object {\n\n\n\n}");
    file.setText(sb.toString());
    return file;
}

/**
 * Override to set selection using browser.
 */
private void setTextSel(int aStart, int anEnd)
{
    String urls = getFile().getURL().getString() + String.format("#Sel=%d-%d", aStart, anEnd);
    getBrowser().setURLString(urls);
}

/**
 * Override to open declaration.
 */
private void openDeclaration(JNode aNode)
{
    JavaDecl decl = aNode.getDecl();
    if(decl!=null) openDecl(decl);
}

/**
 * Open a super declaration.
 */
private void openSuperDeclaration(JMemberDecl aMemberDecl)
{
    JavaDecl sdecl = aMemberDecl.getSuperDecl(); if(sdecl==null) return;
    openDecl(sdecl);
}

/**
 * Opens a project declaration.
 */
private void openDecl(JavaDecl aDecl)
{
    // Get class name (if Java class, open Java source)
    String cname = aDecl.getClassName(); if(cname==null) return;
    if(cname.startsWith("java.") || cname.startsWith("javax.") || cname.startsWith("javafx.")) {
        openJavaDecl(aDecl); return; }

    // Open decl in project file
    Project proj = Project.get(getSite()); if(proj==null) return;
    WebFile file = proj.getProjectSet().getJavaFile(cname); if(file==null) return;
    JavaData jdata = JavaData.get(file);
    JNode node = JavaDeclOwner.getDeclMatch(jdata.getJFile(), aDecl);
    String urls = file.getURL().getString();
    if(node!=null) urls += String.format("#Sel=%d-%d", node.getStart(), node.getEnd());
    getBrowser().setURLString(urls);
}

/**
 * Override to open declaration.
 */
private void openJavaDecl(JavaDecl aDecl)
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
private void showReferences(JNode aNode)
{
    if(getAppPane()==null) return;
    getAppPane().getSearchPane().searchReference(aNode);
    getAppPane().setSupportTrayIndex(SupportTray.SEARCH_PANE);
}

/**
 * Show declarations for given node.
 */
private void showDeclarations(JNode aNode)
{
    if(getAppPane()==null) return;
    getAppPane().getSearchPane().searchDeclaration(aNode);
    getAppPane().setSupportTrayIndex(SupportTray.SEARCH_PANE);
}

/** Override to update Page.Modified. */
private void setTextModified(boolean aFlag)  { getFile().setUpdater(aFlag? this : null); }

/** WebFile.Updater method. */
public void updateFile(WebFile aFile)  { getFile().setText(getTextArea().getText()); }

/** Override to get ProgramCounter from ProcPane. */
private int getProgramCounterLine()
{
    AppPane ap = getAppPane(); return ap!=null? ap.getProcPane().getProgramCounter(getFile()) : -1;
}

/**
 * A JavaTextPane for a JavaPage to implement symbol features and such.
 */
public class JPJavaTextPane extends JavaTextPane {

    /** Override to set selection using browser. */
    public void setTextSel(int aStart, int anEnd)  { JavaPage.this.setTextSel(aStart, anEnd); }
    
    /** Override to open declaration. */
    public void openDeclaration(JNode aNode)  { JavaPage.this.openDeclaration(aNode); }
    
    /** Open a super declaration. */
    public void openSuperDeclaration(JMemberDecl aMemberDecl)  { JavaPage.this.openSuperDeclaration(aMemberDecl); }
    
    /** Show references for given node. */
    public void showReferences(JNode aNode)  { JavaPage.this.showReferences(aNode); }
    
    /** Show declarations for given node. */
    public void showDeclarations(JNode aNode)  { JavaPage.this.showDeclarations(aNode); }
    
    /** Override to update Page.Modified. */
    public void setTextModified(boolean aFlag)  { super.setTextModified(aFlag); JavaPage.this.setTextModified(aFlag); }
    
    /** Override to get ProgramCounter from ProcPane. */
    public int getProgramCounterLine()  { return JavaPage.this.getProgramCounterLine(); }
}

}