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
    // Create SplitView for left/right TextViews, and ScrollView to hold it
    _splitView = new SplitView();
    ScrollView spane = new ScrollView(_splitView); spane.setFillWidth(true); spane.setGrowWidth(true);
    
    // Wrap ScrollView and OverviewPane in HBox and return
    HBox hbox = new HBox(); hbox.setFillHeight(true); hbox.setChildren(spane, new OverviewPane());
    return hbox;
}

/**
 * Initialize UI.
 */
protected void initUI()
{
    // Get texts, initialize and install
    WebFile lfile = getLocalFile(), rfile = getRemoteFile();
    _ltext = getText(lfile); _ltext.setGrowWidth(true);
    _rtext = getText(rfile); _rtext.setGrowWidth(true);
    
    // Get DiffPane and install texts
    _splitView.setItems(_ltext, _rtext);
    
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
static Color fc = new Color(230,230,230,192), sc = new Color(140,140,140);

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

// Colors
static final Color  _marker = new Color(181, 214, 254, 255), _markerBorder = _marker.darker();

/**
 * A component to show locations of Errors, warnings, selected symbols, etc.
 */
public class OverviewPane extends View {

    // The list of markers
    List <Marker>       _markers;
    
    // The last mouse point
    double              _mx, _my;
    
    /** Creates a new OverviewPane. */
    public OverviewPane()  { enableEvents(MouseMove, MouseRelease); setToolTipEnabled(true); setPrefWidth(14); }

    /** Sets the JavaTextView selection. */
    public void setTextSel(int aStart, int anEnd)  { _ltext.setSel(aStart, anEnd); }
    
    /** Returns the list of markers. */
    public List <Marker> getMarkers()
    {
        // If already set, just return
        if(_markers!=null) return _markers;
    
        // Create list
        List <Marker> markers = new ArrayList();
        
        // Add markers for TextView.JavaSource.BuildIssues
        List <TextSel> ranges = _ltext instanceof DiffTextView? ((DiffTextView)_ltext).ranges :
            ((DiffJavaTextView)_ltext).ranges;
        for(TextSel ts : ranges)
            markers.add(new Marker(ts));
        
        // Return markers
        return _markers = markers;
    }

    /** Called on mouse click to select marker line. */
    protected void processEvent(ViewEvent anEvent)
    {
        // Handle MosueClicked
        if(anEvent.isMouseClick()) {
            for(Marker m : getMarkers()) {
                if(m.contains(anEvent.getX(), anEvent.getY())) {
                    setTextSel(m.getSelStart(), m.getSelEnd()); return; }}
            TextBoxLine line = _ltext.getTextBox().getLineForY(anEvent.getY()/getHeight()*_ltext.getHeight());
            setTextSel(line.getStart(), line.getEnd());
        }
        
        // Handle MouseMoved
        if(anEvent.isMouseMove()) {
            _mx = anEvent.getX(); _my = anEvent.getY();
            for(Marker m : getMarkers())
                if(m.contains(_mx, _my)) {
                    setCursor(Cursor.HAND); return; }
            setCursor(Cursor.DEFAULT);
        }
    }

    /** Paint markers. */
    protected void paintFront(Painter aPntr)
    {
        double th = _ltext.getHeight(), h = Math.min(getHeight(), th);
        aPntr.setStroke(Stroke.Stroke1);
        for(Marker m : getMarkers()) {
            m.setY(m._y/th*h);
            aPntr.setPaint(_marker); aPntr.fill(m);
            aPntr.setPaint(_markerBorder); aPntr.draw(m);
        }
    }

    /** Override to return tool tip text. */
    public String getToolTip(ViewEvent anEvent)
    {
        // If marker, return special tooltip
        List <Marker> markers = getMarkers();
        for(int i=0,iMax=markers.size(); i<iMax; i++) { Marker marker = markers.get(i); if(marker.contains(_mx, _my))
            return String.format("Diff %d of %d, Line %d", i+1, iMax, marker._sel.getStartLine().getIndex()); }
        
        // Otherwise, just return line
        TextBoxLine line = _ltext.getTextBox().getLineForY(_my/getHeight()*_ltext.getHeight());
        return "Line: " + (line.getIndex()+1);
    }
}

/**
 * The class that describes a overview marker.
 */
public class Marker <T> extends Rect {

    // The diff range and Y location of range start line in text box.
    TextSel _sel; double  _y;
    
    /** Creates a new marker for target. */
    public Marker(TextSel aSel)
    {
        _sel = aSel; setRect(3,0,10,5);
        TextBoxLine line = aSel.getStartLine();
        _y = line.getY() + line.getHeight()/2;
    }

    /** Returns the selection start. */
    public int getSelStart()  { return _sel.getStart(); }
    
    /** Returns the selection start. */
    public int getSelEnd()  { return _sel.getEnd(); }
}

}