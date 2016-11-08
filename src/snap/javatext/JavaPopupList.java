/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snap.javatext;
import java.util.List;
import snap.gfx.*;
import snap.javaparse.*;
import snap.util.*;
import snap.view.*;

/**
 * A PopupList for an JavaTextPane.
 */
public class JavaPopupList extends PopupList <JavaDecl> implements PropChangeListener {

    // The JavaTextView
    JavaTextView           _textView;
    
    // The current selection start
    int                    _selStart;
    
/**
 * Creates a new java popup for given JavaTextView.
 */
public JavaPopupList(JavaTextView aJavaTextView)
{
    _textView = aJavaTextView;
    setPrefWidth(500);
}

/**
 * Returns the JavaTextView.
 */
public JavaTextView getTextView()  { return _textView; }

/**
 * Applies the current suggestion.
 */
public void applySuggestion()  { applySuggestion(getSelectedItem()); }

/**
 * Applies a suggestion.
 */
public void applySuggestion(JavaDecl aDecl)
{
    // Add suggestion text
    JavaTextView textView = getTextView();
    JNode selectedNode = textView.getSelectedNode();
    String completion = aDecl.getReplaceString();
    int selStart = selectedNode.getStart();
    textView.replaceChars(completion, null, selStart, textView.getSelEnd(), false);
    int argStart = completion.indexOf('('), argEnd = argStart>0? completion.indexOf(')', argStart) : -1;
    if(argEnd>argStart+1) textView.setSel(selStart + argStart + 1, selStart + argEnd);
    
    // Add import for suggestion Class, if not present
    addImport(aDecl, selectedNode.getFile());
    
    // Hide PopupList
    hide(); //textView.requestFocus();
}

/**
 * Inserts the import statement for suggestion into text, if missing.
 */
protected void addImport(JavaDecl aDecl, JFile aFile)
{
    // Handle ClassName suggestion
    if(aDecl.isClass() || aDecl.isConstructor()) {
        String cname = aDecl.getClassName(), csname = aDecl.getSimpleName();
        String cname2 = aFile.getImportClassName(csname);
        if(cname2==null || !cname2.equals(cname)) {
            String cpath = cname.replace('$', '.'), istring = "import " + cpath + ";\n";
            List <JImportDecl> imports = aFile.getImportDecls();
            int is = aFile.getPackageDecl()!=null? aFile.getPackageDecl().getLineIndex() + 1 : 0;
            for(JImportDecl i : imports) {
                if(cpath.compareTo(i.getName())<0) break;
                else is = i.getLineIndex() + 1;
            }
            TextBoxLine line = getTextView().getLine(is);
            getTextView().replaceChars(istring, null, line.getStart(), line.getStart(), false);
        }
    }
}

/**
 * Override to register for TextView property change.
 */
public void show(View aView, double aX, double aY)
{
    super.show(aView, aX, aY);
    _textView.addPropChangeListener(this);
    _selStart = _textView.getSelStart();
}

/**
 * Override to unregister property change.
 */
public void hide()
{
    super.hide();
    _textView.removePropChangeListener(this);
}

/**
 * Catch TextEditor Selection changes that should cause Popup to close.
 */
public void propertyChange(PropChange anEvent)
{
    // If not showing, unregister (in case we were PopupList was dismissed without hide)
    if(!isShowing()) { _textView.removePropChangeListener(this); return; }
    
    // If Selection change, update or hide
    if(anEvent.getPropertyName().equals("Selection")) {
        int start = _textView.getSelStart(), end = _textView.getSelEnd();
        if(start!=end || !(start==_selStart+1 || start==_selStart-1))
            hide();
        _selStart = start;
    }
}

/**
 * Override to select first item and resize.
 */
public void setItems(List <JavaDecl> theItems)
{
    super.setItems(theItems);
    if(theItems!=null && theItems.size()>0)
        setSelectedIndex(0);
}

/**
 * Override to limit pref height.
 */
public double getScrollPrefHeight()  { return getRowHeight()*15; }

/**
 * Override to configure cells.
 */
protected void configureCell(ListCell <JavaDecl> aCell)
{
    super.configureCell(aCell);
    JavaDecl item = aCell.getItem(); if(item==null) return;
    aCell.setText(item.getSuggestionString());
    aCell.setImage(getSuggestionImage(item));
}

/**
 * Override to apply suggestion.
 */
public void fireActionEvent()  { applySuggestion(); }

/**
 * Returns an icon for suggestion.
 */
public static Image getSuggestionImage(JavaDecl aDecl)
{
    switch(aDecl.getType()) {
        case VarDecl: return JavaTextBox.LVarImage;
        case Field: return JavaTextBox.FieldImage;
        case Method: return JavaTextBox.MethodImage;
        case Class: return JavaTextBox.ClassImage;
        case Package: return JavaTextBox.PackageImage;
        default: return null;
    }
}

}