package snap.javatext;
import snap.gfx.TextBoxLine;
import snap.javaparse.*;
import snap.project.*;
import snap.util.*;
import snap.view.View;
import snap.viewx.WebPage;
import snap.web.*;

/**
 * A JavaPage subclass to view/edit Java files.
 */
public class JavaPage extends WebPage {

    // The JavaTextPane
    JavaTextPane           _jtextPane = new JavaTextPane();

/**
 * Returns the JavaTextView.
 */
public JavaTextPane getTextPane()  { return _jtextPane; }

/**
 * Returns the JavaTextView.
 */
public JavaTextView getTextView()  { getUI(); return getTextPane().getTextView(); }

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
    getTextView().setSource(getFile());
    setFirstFocus(getTextView());
}

/**
 * Override to set parameters.
 */
public void setResponse(WebResponse aResp)
{
    // Do normal version
    super.setResponse(aResp);
    
    // Look for LineNumber
    WebURL aURL = aResp.getRequestURL();
    String lineNumberString = aURL.getRefValue("LineNumber");
    if(lineNumberString!=null) { int lineNumber = SnapUtils.intValue(lineNumberString);
        getTextView().selectLine(lineNumber-1); }
    
    // Look for Sel (selection)
    String sel = aURL.getRefValue("Sel");
    if(sel!=null) {
        int start = SnapUtils.intValue(sel); sel = sel.substring(sel.indexOf('-')+1);
        int end = SnapUtils.intValue(sel); if(end<start) end = start;
        getTextView().setSel(start, end);
    }
        
    // Look for SelLine (select line)
    String selLine = aURL.getRefValue("SelLine");
    if(selLine!=null) { int lineNum = SnapUtils.intValue(selLine)-1;
        TextBoxLine tline = lineNum>=0 && lineNum<getTextView().getLineCount()? getTextView().getLine(lineNum) : null;
        if(tline!=null) getTextView().setSel(tline.getStart());
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
            getTextView().setSel(id.getStart(), id.getEnd());
    }
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

}