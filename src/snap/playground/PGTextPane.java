package snap.playground;
import snap.gfx.*;
import snap.view.*;
import snap.viewx.CodeView;
import snap.viewx.TextPane;

/**
 * A custom class.
 */
public class PGTextPane extends TextPane {
    
    // The Playground
    Playground       _pg;
    
    // The TextView
    TextView         _textView;
    
    // LineNumView
    LineNumView      _lineNumView = new LineNumView();

    // EvalView
    EvalView         _evalView = new EvalView();
    
    // Font
    Font             _defaultFont;

/**
 * Creates a new PGTextPane.
 */
public PGTextPane(Playground aPG)  { _pg = aPG; }

/**
 * Creates the TextView.
 */
protected TextView createTextView()  { return new CodeView(); }

/**
 * Returns the default font.
 */
public Font getDefaultFont()
{
    if(_defaultFont==null) {
        String names[] = { "Monaco", "Consolas", "Courier" };
        for(int i=0; i<names.length; i++) {
            _defaultFont = new Font(names[i], 12);
            if(_defaultFont.getFamily().startsWith(names[i]))
                break;
        }
    }
    return _defaultFont;
}

/**
 * Initialize UI.
 */
protected void initUI()
{
    super.initUI();
    View ui = getUI(); ui.setPrefSize(800,700); ui.setGrowHeight(true);
    
    StringBuilder sb = new StringBuilder("// Playground: Play with Java\n\n");
    sb.append("System.out.println(\"Hi Wo!\");").append("\n\n");
    sb.append("x = 1").append("\n\n");
    sb.append("y = x + 1").append("\n\n");
    sb.append("\"Hello\" + \"World\"").append("\n\n");
    sb.append("getClass().getName()").append("\n\n");

    _textView = getTextView(); _textView.setGrowWidth(true);
    _textView.getRichText().setDefaultStyle(new TextStyle(getDefaultFont()));
    _textView.setText(sb.toString());
    ScrollView scroll = _textView.getParent(ScrollView.class);
    
    _textView.getRichText().addPropChangeListener(pce -> _lineNumView.updateLines());
    _lineNumView.updateLines();
    
    RectView rview = new RectView(0,0,1,300); rview.setFill(Color.LIGHTGRAY);
    
    HBox hbox = new HBox(); hbox.setFillHeight(true); hbox.setGrowHeight(true);
    hbox.setChildren(_lineNumView, _textView, rview, _evalView);
    scroll.setContent(hbox);
}

/**
 * A View subclass to show line numbers.
 */
protected class LineNumView extends TextView {
    
    /** Creates new LineNumView. */
    public LineNumView()
    {
        setRich(false);
        getRichText().setDefaultLineStyle(TextLineStyle.DEFAULT.copyFor(HPos.RIGHT));
        setFill(new Color("#f7f7f7"));
        setTextFill(Color.GRAY);
        setPrefWidth(25); setPadding(2,4,2,2);
        setEditable(false); setFont(PGTextPane.this.getDefaultFont());
    }
    
    /** Called to update when textView changes. */
    void updateLines()
    {
        StringBuilder sb = new StringBuilder();
        for(int i=1,iMax=_textView.getLineCount();i<=iMax;i++)
            sb.append(i).append('\n');
        setText(sb.toString());
    }
}

/**
 * A View subclass to show code evaluation.
 */
protected class EvalView extends TextView {
    
    /** Creates new EvalView. */
    public EvalView()
    {
        setFill(new Color("#f7f7f7"));
        setTextFill(Color.GRAY);
        setPrefWidth(200);
        setEditable(false); setFont(PGTextPane.this.getDefaultFont());
        setWrapText(true);
    }
    
    /** Called to update when textView changes. */
    void updateLines()
    {
        StringBuilder sb = new StringBuilder();
        Object lineVals[] = _pg._evaluator._lineVals;
        for(int i=0,iMax=lineVals.length;i<iMax;i++) {
            Object val = lineVals[i];
            if(val!=null)
                sb.append(val);
            sb.append('\n');
        }
        setText(sb.toString());
    }
}

}