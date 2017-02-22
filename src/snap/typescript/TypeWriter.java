package snap.typescript;
import java.util.List;
import snap.javaparse.*;

/**
 * A class to convert Java to TypeScript.
 */
public class TypeWriter extends TypeBuffer {

/**
 * Returns the string for JNode.
 */
public String getString(JNode aNode)
{
    _sb.setLength(0); _indent = 0;
    writeJNode(aNode);
    return _sb.toString();
}

/**
 * Writes a JNode.
 */
public void writeJNode(JNode aNode)
{
    if(aNode instanceof JFile)
        writeJFile((JFile)aNode);
}

/**
 * Write a JFile.
 */
public void writeJFile(JFile aJFile)
{
    append("/* Generated from Java with SnapCode - http://www.reportmill.com */\n");
    
    String pname = aJFile.getPackageName();
    if(pname!=null) append("namespace ").append(pname).append(' ').append("{").endln();
    indent();
    
    // Append imports
    for(JImportDecl imp : aJFile.getImportDecls())
        writeJImportDecl(imp);
    endln();
    
    // Append class decls
    writeJClassDecl(aJFile.getClassDecl());
    
    // Outdent and terminate namespace
    outdent();
    append("}");
    
    // Write main method
    endln().endln().append(aJFile.getClassDecl().getClassName()).append(".main(null);\n");
}

/**
 * Write a JImportDecl.
 */
public void writeJImportDecl(JImportDecl anImp)
{
    String iname = anImp.getName(); if(iname.startsWith("jsweet.")) return;
    int ind = iname.lastIndexOf('.');
    String iname2 = anImp.isInclusive() || ind<0? iname.replace('.', '_') : iname.substring(ind+1);
    append("import ").append(iname2).append(" = ").append(iname).append(';').endln();
}

/**
 * Writes a JClassDecl.
 */
public void writeJClassDecl(JClassDecl aCDecl)
{
    // Append class
    append("export class ").append(aCDecl.getSimpleName()).append(" {").endln().endln();
    indent();
    
    // Append fields
    JFieldDecl fdecls[] = aCDecl.getFieldDecls();
    for(JFieldDecl fd : aCDecl.getFieldDecls()) {
        writeJFieldDecl(fd); endln(); }
        
    // Append methods
    JMethodDecl mdecls[] = aCDecl.getMethodDecls(), mlast = mdecls.length>0? mdecls[mdecls.length-1] : null;
    for(JMethodDecl md : aCDecl.getMethodDecls()) {
        writeJMethodDecl(md); if(md!=mlast) endln(); }
        
    // Terminate
    outdent();
    append('}').endln();
    
    // Write class
    endln();
    append(aCDecl.getSimpleName()).append("[\"__class\"] = \"").append(aCDecl.getClassName()).append("\";").endln();
}

/**
 * Writes a JFieldDecl.
 */
public void writeJFieldDecl(JFieldDecl aFDecl)
{
    // Get modifier string
    JModifiers mods = aFDecl.getModifiers();
    String mstr = mods!=null && mods.isPublic()? "public " : "";

    // Get first var decl
    JVarDecl vd = aFDecl.getVarDecls().get(0);
    
    // Write var decl and terminator
    writeJVarDecl(vd);
    append(';').endln();
}

/**
 * Writes a JMethodDecl.
 */
public void writeJMethodDecl(JMethodDecl aMDecl)
{
    // Get modifier string
    JModifiers mods = aMDecl.getModifiers();
    String mstr = mods!=null && mods.isPublic() && mods.isStatic()? "public static " :
        mods!=null && mods.isPublic()? "public " : "";

    // Write modifiers, method name and args start char
    append(mstr).append(aMDecl.getName()).append("(");
    
    // Write parameters
    List <JVarDecl> params = aMDecl.getParameters();
    JVarDecl last = params.size()>0? params.get(params.size()-1) : null;
    for(JVarDecl param : aMDecl.getParameters()) {
        writeJVarDecl(param); if(param!=last) append(", "); }
        
    // Write parameters close char
    append(") ");
    
    // Write return type (if not empty/void)
    String tstr = getTypeString(aMDecl.getType());
    if(tstr.length()>0) append(": ").append(tstr).append(' ');
    
    // Write method block
    writeJStmtBlock(aMDecl.getBlock(), false);
}

/**
 * Writes a JVarDecl.
 */
public void writeJVarDecl(JVarDecl aVD)
{
    // Write name
    append(aVD.getName()).append(" : ");
    
    // Write type
    String tstr = getTypeString(aVD.getType());
    for(int i=0,iMax=aVD.getArrayCount();i<iMax;i++) tstr += "[]";
    append(tstr).append(' ');
    
    // Write initializer
    JExpr init = aVD.getInitializer();
    if(init!=null) {
        append("= ");
        writeJExpr(init);
    }
}

/**
 * Writes a type.
 */
public void writeJType(JType aType)
{
    String str = getTypeString(aType); if(str.length()==0) return;
    append(str);
}

/**
 * Returns a type string.
 */
public String getTypeString(JType aType)
{
    String name = aType.getName();
    if(name.equals("void")) name = "";
    else if(name.equals("Object")) name = "any";
    else if(name.equals("String")) name = "string";
    else if(aType.isNumberType()) name = "number";
    if(aType.isArrayType())
        name = name + "[]";
    return name;
}

/**
 * Writes a JStmt.
 */
public void writeJStmt(JStmt aStmt)
{
    if(aStmt instanceof JStmtAssert) writeJStmtAssert((JStmtAssert)aStmt);
    else if(aStmt instanceof JStmtBlock) writeJStmtBlock((JStmtBlock)aStmt, false);
    else if(aStmt instanceof JStmtExpr) writeJStmtExpr((JStmtExpr)aStmt);
    else if(aStmt instanceof JStmtFor) writeJStmtFor((JStmtFor)aStmt);
    else if(aStmt instanceof JStmtIf) writeJStmtIf((JStmtIf)aStmt);
    else if(aStmt instanceof JStmtReturn) writeJStmtReturn((JStmtReturn)aStmt);
    else if(aStmt instanceof JStmtSwitch) writeJStmtSwitch((JStmtSwitch)aStmt);
    else if(aStmt instanceof JStmtSynchronized) writeJStmtSynchronized((JStmtSynchronized)aStmt);
    else if(aStmt instanceof JStmtTry) writeJStmtTry((JStmtTry)aStmt);
    else if(aStmt instanceof JStmtVarDecl) writeJStmtVarDecl((JStmtVarDecl)aStmt);
    else append(aStmt.getString()).endln();
}

/**
 * Writes a JStmtAssert.
 */
public void writeJStmtAssert(JStmtAssert aStmt)
{
    JExpr cond = aStmt.getConditional(), expr = aStmt.getExpr();
    append("if(!("); writeJExpr(cond); append(")) ");
    append("throw new Error(\"Assertion error\")");
    append(';').endln();
}

/**
 * Writes a JStmtBlock.
 */
public void writeJStmtBlock(JStmtBlock aBlock, boolean doSemicolon)
{
    // Write start and indent
    append('{').endln();
    indent();
    
    // Write statements
    if(aBlock!=null)
        for(JStmt stmt : aBlock.getStatements())
            writeJStmt(stmt);
        
    // Outdent and terminate
    outdent(); append(doSemicolon? "};" : "}").endln();
}

/**
 * Writes a JStmtExpr.
 */
public void writeJStmtExpr(JStmtExpr aStmt)
{
    writeJExpr(aStmt.getExpr());
    append(';').endln();
}

/**
 * Writes a JStmtIf.
 */
public void writeJStmtIf(JStmtIf aStmt)
{
    // Write if(), conditional and statement
    append("if(");
    writeJExpr(aStmt.getConditional());
    append(") ");
    writeJStmt(aStmt.getStatement());
    
    // Write else
    if(aStmt.getElseStatement()!=null) {
        append("else ");
        writeJStmt(aStmt.getElseStatement());
    }
}

/**
 * Writes a JStmtFor.
 */
public void writeJStmtFor(JStmtFor aStmt)
{
    JStmtVarDecl init = aStmt.getInitDecl();
    JVarDecl initVD = init!=null? init.getVarDecls().get(0) : null;
    JExpr cond = aStmt.getConditional();
    
    append("for(");

    // Handle for(each)
    if(aStmt.isForEach()) {
        append("let ").append(initVD.getName()).append(" of ");
        writeJExpr(cond);
    }
    
    // Handle conventional for()
    else {
        if(initVD!=null) {
            if(initVD.getType()!=null) append("let ");
            writeJVarDecl(initVD);
        }
        append(';');
        if(cond!=null)
            writeJExpr(cond);
        append(';');
        List <JStmtExpr> updStmts = aStmt.getUpdateStmts();
        JStmtExpr last = updStmts.size()>0? updStmts.get(updStmts.size()-1) : null;
        for(JStmtExpr updStmt : updStmts) {
            writeJExpr(updStmt.getExpr()); if(updStmt!=last) append(", "); }
    }

    // Write for closing paren and statement    
    append(") ");
    writeJStmt(aStmt.getStatement());
}

/**
 * Writes a JStmtReturn.
 */
public void writeJStmtReturn(JStmtReturn aStmt)
{
    append("return");
    JExpr expr = aStmt.getExpr();
    if(expr!=null) { append(' '); writeJExpr(expr); }
    append(';').endln();
}

/**
 * Writes a JStmtSwitch.
 */
public void writeJStmtSwitch(JStmtSwitch aStmt)
{
    JExpr expr = aStmt.getExpr();
    append("switch("); writeJExpr(expr); append(") {").endln();
    indent();
    for(JStmtSwitch.SwitchLabel lbl : aStmt.getSwitchLabels())
        writeJStmtSwitchLabel(lbl);
    outdent();
    append('}').endln();
}

/**
 * Writes a JStmtSwitch.
 */
public void writeJStmtSwitchLabel(JStmtSwitch.SwitchLabel aSL)
{
    if(aSL.isDefault()) append("default: ");
    else { append("case "); writeJExpr(aSL.getExpr()); append(": "); }
    for(JStmt stmt : aSL.getStatements())
        writeJStmt(stmt);
}

/**
 * Writes a JStmtSynchronized.
 */
public void writeJStmtSynchronized(JStmtSynchronized aStmt)
{
    writeJStmtBlock(aStmt.getBlock(), true);
}

/**
 * Writes a JStmtTry.
 */
public void writeJStmtTry(JStmtTry aStmt)
{
    JStmtBlock tryBlock = aStmt.getTryBlock();
    List <JStmtTry.CatchBlock> catchBlocks = aStmt.getCatchBlocks();
    JStmtBlock finBlock = aStmt.getFinallyBlock();

    // Write try block
    append("try "); writeJStmtBlock(tryBlock, false);
        
    // Write catch blocks
    //for(JStmtTry.CatchBlock cb : catchBlocks) {
    if(catchBlocks.size()>0) { JStmtTry.CatchBlock cb = catchBlocks.get(0);
        append("catch(").append(cb.getParameter().getName()).append(") ");
        writeJStmtBlock(cb.getBlock(), false);
    }
    
    // Write finally block
    if(finBlock!=null) {
        append("finally "); writeJStmtBlock(finBlock, false); }
}

/**
 * Writes a JStmtVarDecl.
 */
public void writeJStmtVarDecl(JStmtVarDecl aStmt)
{
    for(JVarDecl vd : aStmt.getVarDecls()) {
        append("let ");
        writeJVarDecl(vd);
        append(';').endln();
    }
}

/**
 * Writes a JExpr.
 */
public void writeJExpr(JExpr aExpr)
{
    if(aExpr instanceof JExprChain) writeJExprChain((JExprChain)aExpr);
    else if(aExpr instanceof JExpr.CastExpr) writeJExprCast((JExpr.CastExpr)aExpr);
    else if(aExpr instanceof JExprId) writeJExprId((JExprId)aExpr);
    else if(aExpr instanceof JExpr.InstanceOfExpr) writeJExprInstanceOf((JExpr.InstanceOfExpr)aExpr);
    else if(aExpr instanceof JExprLiteral) writeJExprLiteral((JExprLiteral)aExpr);
    else if(aExpr instanceof JExprMath) writeJExprMath((JExprMath)aExpr);
    else if(aExpr instanceof JExprMethodCall) writeJExprMethodCall((JExprMethodCall)aExpr);
    else append(aExpr.getString());
}

/**
 * Writes a JExpr.CastExpr.
 */
public void writeJExprCast(JExpr.CastExpr aExpr)
{
    append('(').append('<');
    writeJType(aExpr.getType());
    append('>');
    writeJExpr(aExpr.getExpr());
    append(')');
}

/**
 * Writes a JExprId.
 */
public void writeJExprId(JExprId aExpr)
{
    append(aExpr.getString());
}

/**
 * Writes a JExpr.InstanceOfExpr.
 */
public void writeJExprInstanceOf(JExpr.InstanceOfExpr aExpr)
{
    // Get the type and type string
    JExpr expr = aExpr.getExpr();
    JType typ = aExpr.getType();
    String tstr = getTypeString(typ);
    
    // Handle primitive types
    if(tstr.equals("number") || tstr.equals("string")) {
        append("typeof "); writeJExpr(expr); append(" === \'").append(tstr).append('\'');
        return;
    }
    
    // Get expr and write: expr!=null && expr instanceof
    writeJExpr(expr); append("!=null && ");
    writeJExpr(expr); append(" instanceof ");
    
    // Get write type
    writeJType(typ);
}

/**
 * Writes a JExprChain.
 */
public void writeJExprChain(JExprChain aExpr)
{
    // If first expression is field or method call, write "this." prefix
    JExpr exp0 = aExpr.getExpr(0);
    JavaDecl jdecl = exp0.getDecl();
    JavaDecl.Type typ = jdecl!=null? jdecl.getType() : null;
    if(typ==JavaDecl.Type.Field || typ==JavaDecl.Type.Method)
        append("this.");
        
    // Write component expressions
    List <JExpr> exprs = aExpr.getExpressions(); JExpr last = exprs.get(exprs.size()-1);
    for(JExpr exp : exprs) {
        writeJExpr(exp); if(exp!=last) append('.'); }
}

/**
 * Writes a JExprLiteral.
 */
public void writeJExprLiteral(JExprLiteral aExpr)
{
    String str = aExpr.getString();
    JExprLiteral.LiteralType typ = aExpr.getLiteralType();
    if(typ==JExprLiteral.LiteralType.Long || typ==JExprLiteral.LiteralType.Double) {
        char c = str.charAt(str.length()-1);
        if(Character.isLetter(c)) str = str.substring(0, str.length()-1);
    }
    else if(typ==JExprLiteral.LiteralType.Character)
        str = '\'' + str + '\'';
    append(str);
}

/**
 * Writes a JExprMath.
 */
public void writeJExprMath(JExprMath aExpr)
{
    JExprMath.Op op = aExpr.getOp();
    
    // Handle basic binary operations
    if(aExpr.getOperandCount()==2) {
        JExpr exp0 = aExpr.getOperand(0), exp1 = aExpr.getOperand(1);
        writeJExpr(exp0);
        append(' ').append(JExprMath.getOpString(op)).append(' ');
        writeJExpr(exp1);
    }
    
    // Handle conditional
    else if(op==JExprMath.Op.Conditional) {
        JExpr exp0 = aExpr.getOperand(0), exp1 = aExpr.getOperand(1), exp2 = aExpr.getOperand(2);
        writeJExpr(exp0);
        append("? ");
        writeJExpr(exp1);
        append(" : ");
        writeJExpr(exp2);
    }
    
    else append(aExpr.getString());
}

/**
 * Writes a JExprMethodCall.
 */
public void writeJExprMethodCall(JExprMethodCall aExpr)
{
    // Append name and open char
    JExpr id = aExpr.getId(); String name = id.getName();
    append(name).append('(');
    
    // Append args
    List <JExpr> args = aExpr.getArgs(); JExpr last = args.size()>0? args.get(args.size()-1) : null;
    for(JExpr arg : aExpr.getArgs()) {
        writeJExpr(arg); if(arg!=last) append(", "); }
        
    // Append close char
    append(')');
}

}