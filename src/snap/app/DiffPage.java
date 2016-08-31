package snap.app;
import java.util.*;
import snap.app.DiffUtil.*;
import snap.gfx.*;
import snap.javatext.JavaTextView;
import snap.project.VersionControl;
import snap.view.*;
import snap.viewx.WebPage;
import snap.web.WebFile;

/**
 * A WebPage subclass for viewing a DiffFile.
 */
public class DiffPage extends WebPage {

    // The diff pane
    SplitView         _splitView;

    // The two texts
    TextView          _ltext, _rtext;
    
    // The default font
    static Font       _dfont;

/**
 * Creates the UI.
 */
protected View createUI()
{
    _splitView = new SplitView();
    ScrollView spane = new ScrollView(_splitView); spane.setFitWidth(true);
    return spane;
}

/**
 * Initialize UI.
 */
protected void initUI()
{
    // Get texts, initialize and install
    WebFile lfile = getLocalFile(), rfile = getRemoteFile();
    _ltext = getText(lfile);
    _rtext = getText(rfile);
    
    // Get DiffPane and install texts
    _splitView.setChildren(_ltext, _rtext);
    
    // Get ranges lists
    List <TextSel> lranges, rranges;
    lranges = _ltext instanceof DiffTextView? ((DiffTextView)_ltext).ranges : ((DiffJavaTextView)_ltext).ranges;
    rranges = _rtext instanceof DiffTextView? ((DiffTextView)_rtext).ranges : ((DiffJavaTextView)_rtext).ranges;
    
    // Print diffs
    DiffUtil diffUtil = new DiffUtil();
    List <Diff> diffs = diffUtil.diff_main(_rtext.getText(), _ltext.getText());
    int indexL = 0, indexR = 0;
    for(Diff diff : diffs) {
        Operation op = diff.operation;
        boolean insert = op==Operation.INSERT, delete = op==Operation.DELETE;
        int length = diff.text.length();
        if(insert) {
            lranges.add(new TextSel(_ltext.getTextBox(), indexL, indexL + length)); indexL += length; }
        else if(delete) {
            rranges.add(new TextSel(_rtext.getTextBox(), indexR, indexR + length)); indexR += length; }
        else { indexL += length; indexR += length; }
        //String pfx = insert? ">> " : delete? "<< " : ""; Sys.out.println(pfx + diff.text.replace("\n", "\n" + pfx));
    }
}

/**
 * Returns the file local to project.
 */
public WebFile getLocalFile()
{
    String path = getFile().getPath(); path = path.substring(0, path.length() - ".diff".length());
    return getSite().getFile(path);
}

/**
 * Returns the file from Project.RemoteSite.
 */
public WebFile getRemoteFile()
{
    VersionControl vc = VersionControl.get(getFile().getSite());
    return vc.getRepoFile(getLocalFile().getPath(), false, false);
}

/**
 * Returns the text for file.
 */
TextView getText(WebFile aFile)
{
    // Refresh file to get latest version
    aFile.reload();
    
    // Handle JavaFile
    if(aFile.getType().equals("java")) {
        DiffJavaTextView tview = new DiffJavaTextView(); tview.setSource(aFile); return tview; }
    
    // Handle normal TextFile
    TextView ta = new DiffTextView();
    ta.setFont(getDefaultFont());
    ta.setText(aFile.getText());
    return ta;
}

/**
 * Returns the default font.
 */
private Font getDefaultFont()
{
    if(_dfont!=null) return _dfont;
    for(String name : new String[] { "Monaco", "Consolas", "Courier" }) {
        _dfont = new Font(name, 10);
        if(_dfont.getFamily().startsWith(name)) return _dfont;
    }
    return _dfont = new Font("Monospaced", 10);
}

// Stroke and fill colors for diffs
static Color fc = new Color(220,220,220,192), sc = new Color(100,100,100);

/**
 * A text area that shows diffs.
 */
static class DiffTextView extends TextView {

    // The ranges
    List <TextSel> ranges = new ArrayList();
    
    /** Override to add ranges. */
    protected void paintBack(Painter aPntr)
    {
        super.paintBack(aPntr);
        for(TextSel range : ranges) {
            Path rpath = range.getPath();
            aPntr.setPaint(fc); aPntr.fill(rpath); aPntr.setPaint(sc); aPntr.draw(rpath);
        }
    }
}

/**
 * A text area that shows diffs.
 */
static class DiffJavaTextView extends JavaTextView {

    // The ranges
    List <TextSel> ranges = new ArrayList();

    /** Override to add ranges. */
    protected void paintBack(Painter aPntr)
    {
        super.paintBack(aPntr);
        for(TextSel range : ranges) {
            Path rpath = range.getPath();
            aPntr.setPaint(fc); aPntr.fill(rpath); aPntr.setPaint(sc); aPntr.draw(rpath);
        }
    }
}

}