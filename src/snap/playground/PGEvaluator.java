package snap.playground;
import java.io.OutputStream;
import java.io.PrintStream;
import snap.javakit.*;
import snap.view.TextArea;

/**
 * A class to evaluate playground code.
 */
public class PGEvaluator {
    
    // The Playground
    Playground        _pg;
    
    // A Statement evaluator
    EvalStmt          _stmtEval = new EvalStmt();
    
    // An expression evaluator
    EvalExpr          _exprEval;
    
    // The lines
    String            _lines[];
    
    // The values
    Object            _lineVals[];

    // The public out and err PrintStreams
    PrintStream       _sout = System.out, _serr = System.err;
    PrintStream       _pgout = new PGPrintStream(_sout);
    PrintStream       _pgerr = new PGPrintStream(_serr);
    
/**
 * Creates a new PGEvaluator.
 */
public PGEvaluator(Playground aPG)
{
    _pg = aPG;
    _exprEval = EvalExpr.get(_pg);
}

/**
 * Evaluate string.
 */
public void eval(String aStr)
{
    // Set sys out/err to catch console and clear console
    System.setOut(_pgout);
    System.setErr(_pgerr);
    _pg.getConsole().clear();
    
    _lines = aStr.split("\n");
    _lineVals = new Object[_lines.length];
    
    // Iterate over lines and eval each
    for(int i=0, iMax=_lines.length;i<iMax;i++) { String line = _lines[i];
        _lineVals[i] = evalLine(line); }
    
    // Set sys out/err to catch console
    System.setOut(_sout);
    System.setErr(_serr);
}

/**
 * Evaluate string.
 */
protected Object evalLine(String aLine)
{
    // Get trimmed line (just return if empty or comment)
    String line = aLine.trim(); if(line.length()==0 || line.startsWith("//")) return null;
    
    // Get textview and mark current length, in case we need to check for console output
    TextArea tview = _pg.getConsole().getConsoleView();
    int start = tview.length();
    
    // Eval as statement (or expression, if that fails)
    Object val = null;
    try { val = _stmtEval.eval(_pg, line); }
    catch(Exception e) {
        try { val = _exprEval.eval(line); }
        catch(Exception e2) { }
    }
    
    // If val is null, see if there was any console output
    if(val==null) {
        int end = tview.length();
        if(start!=end)
            val = '"' + tview.getText().substring(start,end).trim() + '"';
    }
    
    return val;
}

/**
 * A PrintStream to stand in for System.out and System.err.
 */
private class PGPrintStream extends PrintStream {
    
    /** Creates new PGPrintStream. */
    public PGPrintStream(OutputStream aPS)  { super(aPS); }
    
    /** Override to send to ScanView. */
    public void write(int b)
    {
        super.write(b);
        String str = String.valueOf(Character.valueOf((char)b));
        if(this==_pgout)
            _pg.getConsole().appendOut(str);
        else _pg.getConsole().appendErr(str);
    }
    
    /** Override to send to ScanView. */
    public void write(byte buf[], int off, int len)
    {
        super.write(buf, off, len);
        String str = new String(buf, off, len);
        if(this==_pgout)
            _pg.getConsole().appendOut(str);
        else _pg.getConsole().appendErr(str);
    }
}

}