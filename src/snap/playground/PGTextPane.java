package snap.playground;
import snap.geom.HPos;
import snap.gfx.*;
import snap.text.*;
import snap.view.*;
import snap.viewx.CodeView;
import snap.viewx.TextPane;

/**
 * A custom class.
 */
public class PGTextPane extends TextPane {
    
    // The Playground
    Playground       _pg;
    
    // The TextArea
    TextArea         _textArea;
    
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
 * Creates the TextArea.
 */
protected TextArea createTextArea()  { return new CodeView(); }

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
    
    StringBuilder sb = new StringBuilder();
    sb.append("// \n");
    sb.append("// Playground: Enter Java statements and expressions\n");
    sb.append("//\n\n");
    //sb.append("System.out.println(\"Hello World!\");").append("\n\n");
    sb.append("int x = 1").append("\n\n");
    sb.append("y = x + 1").append("\n\n");
    //sb.append("\"Hello\" + \" Again\"").append("\n\n");
    //sb.append("getClass().getName()").append("\n\n");

    _textArea = getTextArea(); _textArea.setGrowWidth(true);
    _textArea.getRichText().setDefaultStyle(new TextStyle(getDefaultFont()));
    enableEvents(_textArea, KeyRelease);
    _textArea.setText(sb.toString());
    ScrollView scroll = _textArea.getParent(ScrollView.class);
    
    _textArea.getRichText().addPropChangeListener(pce -> _lineNumView.updateLines());
    _lineNumView.updateLines();
    
    RectView rview = new RectView(0,0,1,300); rview.setFill(Color.LIGHTGRAY);
    
    RowView hbox = new RowView(); hbox.setFillHeight(true); hbox.setGrowHeight(true);
    hbox.setChildren(_lineNumView, _textArea, rview, _evalView);
    scroll.setContent(hbox);
}

/**
 * Respond to UI changes.
 */
protected void respondUI(ViewEvent anEvent)
{
    if(anEvent.isKeyRelease() && anEvent.isEnterKey())
        runLater(() -> _pg.play());
        
    else super.respondUI(anEvent);
}

/**
 * A TextArea subclass to show line numbers.
 */
protected class LineNumView extends TextArea {
    
    /** Creates new LineNumView. */
    public LineNumView()
    {
        setPlainText(true);
        setDefaultLineStyle(TextLineStyle.DEFAULT.copyFor(HPos.RIGHT));
        setFill(new Color("#f7f7f7"));
        setTextFill(new Color(.6f));
        setPrefWidth(25); setPadding(2,4,2,2);
        setEditable(false); setFont(PGTextPane.this.getDefaultFont());
    }
    
    /** Called to update when TextArea changes. */
    void updateLines()
    {
        StringBuilder sb = new StringBuilder();
        for(int i=1,iMax=_textArea.getLineCount();i<=iMax;i++)
            sb.append(i).append('\n');
        setText(sb.toString());
    }
}

/**
 * A TextArea subclass to show code evaluation.
 */
protected class EvalView extends TextArea {
    
    /** Creates new EvalView. */
    public EvalView()
    {
        setFill(new Color("#f7f7f7"));
        setTextFill(Color.GRAY);
        setPrefWidth(200);
        setEditable(false); setFont(PGTextPane.this.getDefaultFont());
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