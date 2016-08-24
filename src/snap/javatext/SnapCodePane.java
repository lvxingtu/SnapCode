package snap.javatext;
import snap.view.*;

/**
 * A custom class.
 */
public class SnapCodePane extends ViewOwner {

    // The SnapJavaPane
    SnapJavaPane      _javaPane;

    // The SnapEditorPane
    SnapEditorPane    _editorPane;
    
    // The top level switch view
    SwitchView        _switchView;
    
/**
 * Create new SnapCodePane.
 */
public SnapCodePane()
{
    _javaPane = createJavaPane(); _javaPane._codePane = this;
    _editorPane = createEditorPane(); _editorPane._codePane = this;
    _javaPane._editorPane = _editorPane;
    _editorPane._javaPane = _javaPane;
}

/**
 * Returns the SnapJavaPane.
 */
public SnapJavaPane getJavaPane()  { return _javaPane; }

/**
 * Creates the SnapJavaPane.
 */
protected SnapJavaPane createJavaPane()  { return new SnapJavaPane(); }

/**
 * Returns the JavaTextView.
 */
public JavaTextView getJavaView()  { return getJavaPane().getTextView(); }

/**
 * Creates the SnapEditorPane.
 */
protected SnapEditorPane createEditorPane()  { return new SnapEditorPane(); }

/**
 * Returns whether this SnapEditorPane is showing snap code.
 */
public boolean getShowSnapCode()
{
    return _switchView!=null? _switchView.getSelectedIndex()>0 : getShowSnapCodeDefault();
}

/**
 * Turns on SnapCode mode.
 */
public void setShowSnapCode(boolean isSnapCode)
{
    if(_switchView.getChildren().size()<2) _switchView.addChild(_editorPane.getUI());
    _switchView.setSelectedIndex(isSnapCode? 1 : 0);
}

/**
 * Returns whether this SnapEditorPane is showing snap code.
 */
public boolean getShowSnapCodeDefault()  { return false; }

/**
 * Create UI.
 */
protected View createUI()
{
    _switchView = new SwitchView();
    _switchView.addChild(getJavaPane().getUI());
    return _switchView;
}

/**
 * Initialize UI.
 */
protected void initUI()
{    
    if(getShowSnapCodeDefault())
        setShowSnapCode(true);
}

}