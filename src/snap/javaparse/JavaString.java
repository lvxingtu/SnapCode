package snap.javaparse;
import java.util.List;

/**
 * A custom class.
 */
public class JavaString {

    // The StringBuffer
    StringBuffer _sb = new StringBuffer();
    
    // The indent string
    String       _indentString = "    ";
    
    // The indent level
    int          _indentLevel;

/**
 * Append String.
 */
public JavaString append(String aStr)  { _sb.append(aStr); return this; }

/**
 * Append char.
 */
public JavaString append(char aChar)  { _sb.append(aChar); return this; }

/**
 * Append int.
 */
public JavaString append(int anInt)  { _sb.append(anInt); return this; }

/**
 * Append long.
 */
public JavaString append(long aLong)  { _sb.append(aLong); return this; }

/**
 * Append float.
 */
public JavaString append(float aFloat)  { return append((double)aFloat); }

/**
 * Append double.
 */
public JavaString append(double aDouble)
{
    if(aDouble==(int)aDouble) return append((long)aDouble);
    _sb.append(aDouble); return this;
}

/**
 * Append Object.
 */
public JavaString append(Object anObj)
{
    if(anObj instanceof JNode) append((JNode)anObj);
    _sb.append(anObj); return this;
}

/**
 * Append JNode.
 */
public JavaString append(JNode aNode)
{
    if(aNode instanceof JExpr) return append((JExpr)aNode);
    if(aNode instanceof JModifiers) return append((JModifiers)aNode);
    if(aNode instanceof JType) return append((JType)aNode);
    if(aNode instanceof JStmt) return append((JStmt)aNode);
    if(aNode==null) return append("null");
    return append("JavaString.append(JNode): Not implemented for " + aNode.getClass().getSimpleName());
}

/**
 * Append JModifiers.
 */
public JavaString append(JModifiers theMods)
{
    if(theMods.isAbstract()) append("abstract ");
    if(theMods.isFinal()) append("final ");
    if(theMods.isInterface()) append("interface ");
    if(theMods.isNative()) append("native ");
    if(theMods.isPrivate()) append("private ");
    if(theMods.isProtected()) append("protected ");
    if(theMods.isPublic()) append("public ");
    if(theMods.isStatic()) append("static ");
    if(theMods.isStrict()) append("strict ");
    if(theMods.isSynchronized()) append("synchronized ");
    return this;
}

/**
 * Append JExpr.
 */
public JavaString append(JExpr anExpr)
{
    if(anExpr instanceof JExpr.CastExpr) return append((JExpr.CastExpr)anExpr);
    if(anExpr instanceof JExpr.InstanceOfExpr) return append((JExpr.InstanceOfExpr)anExpr);
    if(anExpr instanceof JExprAlloc) return append((JExprAlloc)anExpr);
    if(anExpr instanceof JExprArrayIndex) return append((JExprArrayIndex)anExpr);
    if(anExpr instanceof JExprChain) return append((JExprChain)anExpr);
    if(anExpr instanceof JExprId) return append((JExprId)anExpr);
    if(anExpr instanceof JExprLambda) return append((JExprLambda)anExpr);
    if(anExpr instanceof JExprLiteral) return append((JExprLiteral)anExpr);
    if(anExpr instanceof JExprMath) return append((JExprMath)anExpr);
    if(anExpr instanceof JExprMethodCall) return append((JExprMethodCall)anExpr);
    if(anExpr instanceof JExprMethodRef) return append((JExprMethodRef)anExpr);
    if(anExpr instanceof JExprType) return append((JExprType)anExpr);
    if(anExpr==null) return append("null");
    return append("JavaString.append(JExpr): Not implemented for " + anExpr.getClass().getSimpleName());
}

/**
 * Append JExpr.CastExpr.
 */
public JavaString append(JExpr.CastExpr anExpr)
{
    return append('(').append(anExpr.getType()).append(')').append(anExpr.getExpr());
}

/**
 * Append JExpr.InstanceOfExpr.
 */
public JavaString append(JExpr.InstanceOfExpr anExpr)
{
    return append(anExpr.getExpr()).append(" instanceof ").append(anExpr.getType());
}

/**
 * Append JExprAlloc.
 */
public JavaString append(JExprAlloc anExpr)
{
    append("new ").append(anExpr.getType()).append(anExpr.getArgs());
    if(anExpr.getClassDecl()!=null) append(anExpr.getClassDecl());
    return this;
}

/**
 * Append JExprArrayIndex.
 */
public JavaString append(JExprArrayIndex anExpr)
{
    return append(anExpr.getArrayExpr()).append('[').append(anExpr.getIndexExpr()).append(']');
}

/**
 * Append JExprChain.
 */
public JavaString append(JExprChain anExpr)
{
    if(anExpr.getExprCount()>0) append(anExpr.getExpr(0));
    for(int i=1, iMax=anExpr.getExprCount();i<iMax;i++)
        append('.').append(anExpr.getExpr(i));
    return this;
}

/**
 * Append JExprId.
 */
public JavaString append(JExprId anExpr)  { return append(anExpr.getName()); }

/**
 * Append JExprLambda.
 */
public JavaString append(JExprLambda anExpr)
{
    int pcount = anExpr.getParamCount();
    if(pcount>1 || pcount>0 && anExpr.getParam(0).isTypeSet()) append("(");
    if(pcount>0) append(anExpr.getParam(0));
    for(int i=1;i<pcount;i++) append(", ").append(anExpr.getParam(i));
    if(pcount>1 || pcount>0 && anExpr.getParam(0).isTypeSet()) append(")");
    append(" -> ");
    if(anExpr.getExpr()!=null) return append(anExpr.getExpr());
    return append(anExpr.getBlock());
}

/**
 * Append JExprLiteral.
 */
public JavaString append(JExprLiteral anExpr)  { return append(anExpr.getValueString()); }

/**
 * Append JExprMath.
 */
public JavaString append(JExprMath aME)
{
    JExpr s1 = aME.getChildCount()>0? aME.getOperand(0) : null;
    JExpr s2 = aME.getChildCount()>1? aME.getOperand(1) : null;
    switch(aME.getOp()) {
        case Add: return append(s1).append('+').append(s2);
        case Subtract: return append(s1).append('-').append(s2);
        case Multiply: return append(s1).append('*').append(s2);
        case Divide: return append(s1).append('/').append(s2);
        case Mod: return append(s1).append('%').append(s2);
        case Equal: return append(s1).append("==").append(s2);
        case NotEqual: return append(s1).append("!=").append(s2);
        case LessThan: return append(s1).append('<').append(s2);
        case GreaterThan:  return append(s1).append('>').append(s2);
        case LessThanOrEqual:  return append(s1).append("<=").append(s2);
        case GreaterThanOrEqual: return append(s1).append(">=").append(s2);
        case Or: return append(s1).append("||").append(s2);
        case And: return append(s1).append("&&").append(s2);
        case Not: return append('!').append(s1);
        case Negate: return append('-').append(s1);
        case Conditional:
            JExpr s3 = aME.getChildCount()>2? aME.getOperand(2) : null;
            return append(s1).append("? ").append(s1).append(" : ").append(s3);
        case Assignment: return append(s1).append(" = ").append(s2);
        case BitOr: return append(s1).append('|').append(s2);
        case BitXOr: return append(s1).append('^').append(s2);
        case BitAnd: return append(s1).append('&').append(s2);
        case ShiftLeft: return append(s1).append("<<").append(s2);
        case ShiftRight: return append(s1).append(">>").append(s2);
        case ShiftRightUnsigned: return append(s1).append(">>>").append(s2);
        case PreIncrement: return append("++").append(s1);
        case PreDecrement:return append("--").append(s1);
        case BitComp: return append('~').append(s1);
        case PostIncrement: return append(s1).append("++");
        case PostDecrement: return append(s1).append("--");
        default: System.err.println("JavaString.append(JExprMath): Unsupported op " + aME.getOp()); return null;
    }
}

/**
 * Append JExprMethodCall.
 */
public JavaString append(JExprMethodCall aMC)  { return append(aMC.getName()).append(aMC.getArgs()); }

/**
 * Append JExprMethodRef.
 */
public JavaString append(JExprMethodRef aMR)  { return append(aMR.getExpr()).append("::").append(aMR.getId()); }

/**
 * Append JExprType.
 */
public JavaString append(JExprType anExpr)  { return append(anExpr.getType()); }

/**
 * Appends parameters.
 */
public JavaString append(List <JExpr> theArgs)
{
    append("(");
    if(theArgs.size()>0) append(theArgs.get(0));
    for(int i=1;i<theArgs.size();i++) append(", ").append(theArgs.get(i));
    return append(")");
}

/**
 * Append JStmt.
 */
public JavaString append(JStmt aStmt)
{
    if(aStmt instanceof JStmtAssert) return append((JStmtAssert)aStmt);
    if(aStmt instanceof JStmtBlock) return append((JStmtBlock)aStmt);
    if(aStmt instanceof JStmtBreak) return append((JStmtBreak)aStmt);
    if(aStmt instanceof JStmtClassDecl) return append((JStmtClassDecl)aStmt);
    if(aStmt instanceof JStmtContinue) return append((JStmtContinue)aStmt);
    if(aStmt instanceof JStmtDo) return append((JStmtDo)aStmt);
    if(aStmt instanceof JStmtEmpty) return append((JStmtEmpty)aStmt);
    if(aStmt instanceof JStmtExpr) return append((JStmtExpr)aStmt);
    if(aStmt instanceof JStmtFor) return append((JStmtFor)aStmt);
    if(aStmt instanceof JStmtIf) return append((JStmtIf)aStmt);
    if(aStmt instanceof JStmtLabeled) return append((JStmtLabeled)aStmt);
    if(aStmt instanceof JStmtReturn) return append((JStmtReturn)aStmt);
    if(aStmt instanceof JStmtSwitch) return append((JStmtSwitch)aStmt);
    if(aStmt instanceof JStmtSynchronized) return append((JStmtSynchronized)aStmt);
    if(aStmt instanceof JStmtThrow) return append((JStmtThrow)aStmt);
    if(aStmt instanceof JStmtTry) return append((JStmtTry)aStmt);
    if(aStmt instanceof JStmtVarDecl) return append((JStmtVarDecl)aStmt);
    if(aStmt instanceof JStmtWhile) return append((JStmtWhile)aStmt);
    if(aStmt instanceof JStmtConstrCall) return append((JStmtConstrCall)aStmt);
    if(aStmt==null) return append("null");
    return append("JavaString.append(JStmt): Not implemented for " + aStmt.getClass().getSimpleName());
}

/**
 * Append JStmtAssert.
 */
public JavaString append(JStmtAssert aStmt)
{
    append("assert ").append(aStmt.getConditional());
    if(aStmt.getExpr()!=null) append(' ').append(aStmt.getExpr());
    return append(';');
}

/**
 * Append JStmtBlock.
 */
public JavaString append(JStmtBlock aStmt)
{
    append("{\n"); indent();
    if(aStmt!=null)
        for(JStmt s : aStmt.getStatements())
            appendIndent().append(s).append('\n');
    outdent(); return append("}");
}

/**
 * Append JStmtBreak.
 */
public JavaString append(JStmtBreak aStmt)
{
    append("break"); if(aStmt.getLabel()!=null) append(' ').append(aStmt.getLabel()); return append(";");
}

/**
 * Append JStmtClassDecl.
 */
public JavaString append(JStmtClassDecl aStmt)
{
    return append("; // JavaString.append(JStmtClassDecl) not implemented");
}

/**
 * Append JStmtContinue.
 */
public JavaString append(JStmtContinue aStmt)
{
    append("continue"); if(aStmt.getLabel()!=null) append(' ').append(aStmt.getLabel()); return append(";");
}

/**
 * Append JStmtDo.
 */
public JavaString append(JStmtDo aStmt)
{
    append("do ");
    append(aStmt.getStatement());
    return append("} while(").append(aStmt.getConditional()).append(");");
}

/**
 * Append JStmtEmpty.
 */
public JavaString append(JStmtEmpty aStmt)  { return append(";"); }

/**
 * Append JStmtExpr.
 */
public JavaString append(JStmtExpr aStmt)  { return append(aStmt.getExpr()).append(";"); }

/**
 * Append JStmtFor.
 */
public JavaString append(JStmtFor aStmt)
{
    append("for(");
    if(aStmt.getInitDecl()!=null || aStmt.getConditional()!=null || aStmt.getUpdateStmts()!=null) {
        if(aStmt.getInitDecl()!=null) append(aStmt.getInitDecl()); else append(";");
        if(aStmt.getConditional()!=null) append(aStmt.getConditional()); append(";");
        if(aStmt.getUpdateStmts()!=null && aStmt.getUpdateStmts().size()>0) {
            append(aStmt.getUpdateStmts().get(0));
            for(int i=1;i<aStmt.getUpdateStmts().size(); i++) append(", ").append(aStmt.getUpdateStmts().get(i));
        }
    }
    else append("JavaString.append(JStmtFor) not implemented for foreach");
    return append(") ").append(aStmt.getStatement());
}

/**
 * Append JStmtIf.
 */
public JavaString append(JStmtIf aStmt)
{
    append("if(").append(aStmt.getConditional()).append(") ").append(aStmt.getStatement());
    if(aStmt.getElseStatement()!=null) append('\n').appendIndent().append("else ").append(aStmt.getElseStatement());
    return this;
}

/**
 * Append JStmtLabeled.
 */
public JavaString append(JStmtLabeled aStmt)
{
    append(aStmt.getLabel()).append(": ");
    if(aStmt.getStmt()!=null) append(aStmt.getStmt());
    return this;
}

/**
 * Append JStmtReturn.
 */
public JavaString append(JStmtReturn aStmt)
{
    append("return ");
    if(aStmt.getExpr()!=null) append(aStmt.getExpr());
    return append(";");
}

/**
 * Append JStmtSwitch.
 */
public JavaString append(JStmtSwitch aStmt)
{
    append("switch(").append(aStmt.getExpr()).append(") {"); indent();
    for(JStmtSwitch.SwitchLabel label : aStmt.getSwitchLabels()) {
        appendIndent().append("case ").append(label.getExpr()).append(": ");
        for(JStmt stmt : label.getStatements()) append(stmt).append('\n');
    }
    outdent(); return append("\n}");
}

/**
 * Append JStmtSynchronized.
 */
public JavaString append(JStmtSynchronized aStmt)
{
    return append("synchronized ").append(aStmt.getBlock());
}

/**
 * Append JStmtThrow.
 */
public JavaString append(JStmtThrow aStmt)
{
    return append("throw ").append(aStmt.getExpr()).append(";");
}

/**
 * Append JStmtTry.
 */
public JavaString append(JStmtTry aStmt)
{
    append("try ").append(aStmt.getTryBlock());
    for(JStmtTry.CatchBlock c : aStmt.getCatchBlocks())
        append("\ncatch(").append(c.getParameter()).append(") ").append(c.getBlock());
    if(aStmt.getFinallyBlock()!=null) append("finally ").append(aStmt.getFinallyBlock());
    return this;
}

/**
 * Append JStmtVarDecl.
 */
public JavaString append(JStmtVarDecl aStmt)
{
    append(aStmt.getType()).append(' ');
    if(aStmt.getVarDecls().size()>0) append(aStmt.getVarDecls().get(0));
    for(int i=1;i<aStmt.getVarDecls().size();i++) append(", ").append(aStmt.getVarDecls().get(i));
    return append(";");
}

/**
 * Append JStmtWhile.
 */
public JavaString append(JStmtWhile aStmt)
{
    return append("while(").append(aStmt.getConditional()).append(") ").append(aStmt.getStmt());
}

/**
 * Append JConstrCall.
 */
public JavaString append(JStmtConstrCall aStmt)
{
    List <JExprId> idList = aStmt.getIds(); if(idList.size()>0) append(idList.get(0));
    for(int i=1;i<idList.size();i++) append('.').append(idList.get(i));
    return append(aStmt.getArgs());
}

/**
 * Append JVarDecl.
 */
public JavaString append(JType aType)
{
    append(aType.getName());
    for(int i=0;i<aType.getArrayCount();i++) append("[]");
    return this;
}

/**
 * Append JVarDecl.
 */
public JavaString append(JVarDecl aVD)
{
    append(aVD.getName());
    for(int i=0;i<aVD.getArrayCount();i++) append("[]");
    if(aVD.getInitializer()!=null) append(" = ").append(aVD.getInitializer());
    if(aVD.getArrayInit()!=null) append(" = ").append(aVD.getArrayInit());
    return this;
}

/**
 * Increases the indent level.
 */
public void indent()  { _indentLevel++; }

/**
 * Decreases the indent level by one.
 */
public void outdent()  { _indentLevel--; }

/**
 * Appends the indent.
 */
public JavaString appendIndent() { for(int i=0;i<_indentLevel;i++) append(_indentString); return this; }

/**
 * Standard toString implementation.
 */
public String toString()  { return _sb.toString(); }

/** Trims zero from end of string. */
private static String trimZero(String aStr)
{
    if(aStr.indexOf('.')<0) return aStr;
    while(aStr.endsWith("0")) aStr = aStr.substring(0,aStr.length()-1);
    return aStr;
}

}