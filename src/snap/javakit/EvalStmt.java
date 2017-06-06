package snap.javakit;
import snap.parse.Parser;

/**
 * A class to evaluate Java statements.
 */
public class EvalStmt {
    
    // The Expression evaluator
    EvalExpr            _exprEval = EvalExpr.get(null);

    // A parser to parse expressions
    static Parser       _stmtParser = JavaParser.getShared().getStmtParser();
    
/**
 * Evaluate expression.
 */
public Object eval(String anExpr)
{
    _stmtParser.setInput(anExpr);
    Object oref = _exprEval.thisObject();
    JStmt stmt = _stmtParser.parseCustom(JStmt.class);
    Object value; try { value = evalStmt(oref, stmt); }
    catch(Exception e) { return e; }
    return value;
}

/**
 * Evaluate JStmt.
 */
public Object evalStmt(Object anOR, JStmt aStmt) throws Exception
{
    //if(aStmt instanceof JStmtAssert) return evalJStmtAssert((JStmtAssert)aStmt);
    //else if(aStmt instanceof JStmtBlock) return evalJStmtBlock((JStmtBlock)aStmt, false);
    //else if(aStmt instanceof JStmtBreak) return evalJStmtBreak((JStmtBreak)aStmt);
    //else if(aStmt instanceof JStmtClassDecl) return evalJStmtClassDecl((JStmtClassDecl)aStmt);
    //else if(aStmt instanceof JStmtConstrCall) return evalJStmtConstrCall((JStmtConstrCall)aStmt);
    //else if(aStmt instanceof JStmtContinue) return evalJStmtContinue((JStmtContinue)aStmt);
    //else if(aStmt instanceof JStmtDo) return evalJStmtDo((JStmtDo)aStmt);
    //else if(aStmt instanceof JStmtEmpty) return evalJStmtEmpty((JStmtEmpty)aStmt);
    if(aStmt instanceof JStmtExpr) return evalJStmtExpr((JStmtExpr)aStmt);
    //else if(aStmt instanceof JStmtFor) return evalJStmtFor((JStmtFor)aStmt);
    //else if(aStmt instanceof JStmtIf) return evalJStmtIf((JStmtIf)aStmt);
    //else if(aStmt instanceof JStmtLabeled) return evalJStmtLabeled((JStmtLabeled)aStmt);
    //else if(aStmt instanceof JStmtReturn) return evalJStmtReturn((JStmtReturn)aStmt);
    //else if(aStmt instanceof JStmtSwitch) return evalJStmtSwitch((JStmtSwitch)aStmt);
    //else if(aStmt instanceof JStmtSynchronized) return evalJStmtSynchronized((JStmtSynchronized)aStmt);
    //else if(aStmt instanceof JStmtThrow) return evalJStmtThrow((JStmtThrow)aStmt);
    //else if(aStmt instanceof JStmtTry) return evalJStmtTry((JStmtTry)aStmt);
    else if(aStmt instanceof JStmtVarDecl) return evalJStmtVarDecl((JStmtVarDecl)aStmt);
    //else if(aStmt instanceof JStmtWhile) return evalJStmtWhile((JStmtWhile)aStmt);
    else throw new RuntimeException("EvalStmt.evalStmt: Unsupported statement " + aStmt.getClass());
}

/**
 * Evaluate JStmtExpr.
 */
public Object evalJStmtExpr(JStmtExpr aStmt)
{
    return null;
}

/**
 * Evaluate JStmtVarDecl.
 */
public Object evalJStmtVarDecl(JStmtVarDecl aStmt)
{
    return null;
}



}