package snap.playground;
import snap.javakit.*;

/**
 * A class to evaluate playground code.
 */
public class PGEvaluator {
    
    // The Playground
    Playground        _pg;
    
    // A Statement evaluator
    EvalStmt          _stmtEval = new EvalStmt();
    
    // An expression evaluator
    EvalExpr          _exprEval = EvalExpr.get(this);
    
    // The lines
    String            _lines[];
    
    // The values
    Object            _lineVals[];

/**
 * Evaluate string.
 */
public void eval(String aStr)
{
    _lines = aStr.split("\n");
    _lineVals = new Object[_lines.length];
    
    for(int i=0, iMax=_lines.length;i<iMax;i++) { String line = _lines[i];
        if(line.trim().length()==0) continue;
        Object val = null;
        try { val = _stmtEval.eval(line); }
        catch(Exception e) {
            try { val = _exprEval.eval(line); }
            catch(Exception e2) { }
        }
            
        _lineVals[i] = val;
    }
}

}