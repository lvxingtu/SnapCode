package snap.typescript;
import java.util.*;
import snap.javakit.*;

/**
 * A class to convert Java to TypeScript.
 */
public class TSWriter {

    // The StringBuffer
    StringBuffer      _sb = new StringBuffer();
    
    // The indent string
    String            _indentStr = "    ";
    
    // The indent level
    int               _indent;
    
    // Whether at line end
    boolean           _lineStart;

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
    if(aNode instanceof JFile) writeJFile((JFile)aNode);
    //else if(aNode instanceof JPackageDecl) writeJPackageDecl((JPackageDecl)aNode);
    else if(aNode instanceof JImportDecl) writeJImportDecl((JImportDecl)aNode);
    else if(aNode instanceof JClassDecl) writeJClassDecl((JClassDecl)aNode);
    else if(aNode instanceof JFieldDecl) writeJFieldDecl((JFieldDecl)aNode);
    else if(aNode instanceof JMethodDecl) writeJMethodDecl((JMethodDecl)aNode);
    else if(aNode instanceof JStmt) writeJStmt((JStmt)aNode);
    else if(aNode instanceof JExpr) writeJExpr((JExpr)aNode);
    else if(aNode instanceof JType) writeJType((JType)aNode);
    else if(aNode instanceof JVarDecl) writeJVarDecl((JVarDecl)aNode);
    else if(aNode instanceof JEnumConst) writeJEnumConst((JEnumConst)aNode);
    else append("TSWriter: write" + aNode.getClass().getSimpleName() + " not implemented");
}

/**
 * Writes a list of JNodes joined by string.
 */
public void writeJNodesJoined(List <? extends JNode> theNodes, String aStr)
{
    JNode last = theNodes.size()>0? theNodes.get(theNodes.size()-1) : null;
    for(JNode node : theNodes) {
        writeJNode(node); if(node!=last) append(aStr); }
}

/**
 * Write a JFile.
 */
public void writeJFile(JFile aJFile)
{
    append("/* Generated from Java with SnapCode - http://www.reportmill.com */\n");
    
    String pname = aJFile.getPackageName();
    if(pname!=null)
        append("namespace ").append(pname).append(' ').append("{").endln().indent();
    
    // Append imports
    aJFile.getUnusedImports();
    for(JImportDecl imp : aJFile.getImportDecls())
        writeJImportDecl(imp);
    endln();
    
    // Append class decls
    writeJClassDecl(aJFile.getClassDecl());
    
    // Outdent and terminate namespace
    if(pname!=null)
        outdent().append('}').endln();
    
    // Write main method
    if(aJFile.getClassDecl().getSimpleName().equals("HelloWorld"))
        endln().append(aJFile.getEvalClassName()).append(".main(null);\n");
}

/**
 * Write a JImportDecl.
 */
public void writeJImportDecl(JImportDecl anImp)
{
    String iname = anImp.getName(); if(iname.startsWith("jsweet.")) return;
    iname = iname.replace(".function", ".__function");
    
    // Handle inclusive
    if(anImp.isInclusive()) {
        for(String cname : anImp.getFoundClassNames()) {
            append("import ").append(cname).append(" = ").append(iname).append('.').append(cname).append(';').endln(); }
    }

    // Fully specified    
    else {
        int ind = iname.lastIndexOf('.');
        String cname = iname.substring(ind+1);
        append("import ").append(cname).append(" = ").append(iname).append(';').endln();
    }
}

/**
 * Writes a JClassDecl.
 */
public void writeJClassDecl(JClassDecl aCDecl)
{
    // If enum, go to specific method
    if(aCDecl.isEnum()) {
        writeJClassDeclEnum(aCDecl); return; }

    // Get class name
    String cname = aCDecl.getSimpleName();
    boolean isInterface = aCDecl.isInterface();
    if(aCDecl.isAnonymousClass()) { 
        JClassDecl ec = aCDecl.getEnclosingClassDecl();
        cname = ec.getSimpleName() + '$' + cname;
    }
    
    // Add export call
    if(aCDecl.getFile().getPackageName()!=null)
        append("export ");

    // Append class label with modifiers: public class/interface XXX ...
    JModifiers mods = aCDecl.getMods();
    String mstr = mods!=null && mods.isAbstract()? "abstract " : "";
    append(mstr);
    append(isInterface? "interface " : "class ");
    append(cname).append(' ');
    
    // Append extends types
    List <JType> etypes = aCDecl.getExtendsTypes(); JType elast = etypes.size()>0? etypes.get(etypes.size()-1) : null;
    if(etypes.size()>0) append("extends ");
    for(JType etyp : etypes) {
        writeJType(etyp); if(etyp!=elast) append(", "); else append(' '); }
        
    // Append implements types
    List <JType> itypes = aCDecl.getImplementsTypes(); JType ilast = itypes.size()>0? itypes.get(itypes.size()-1): null;
    if(itypes.size()>0) append("implements ");
    for(JType ityp : itypes) {
        writeJType(ityp); if(ityp!=ilast) append(", "); else append(' '); }
    
    // Write class label close char
    append('{').endln().endln();
    indent();
    
    // Append fields
    JFieldDecl fdecls[] = aCDecl.getFieldDecls();
    if(!isInterface) for(JFieldDecl fd : fdecls) {
        writeJFieldDecl(fd); endln(); }
        
    // Append constructors
    JConstrDecl cndecls[] = aCDecl.getConstructorDecls();
    if(cndecls.length>1) writeJConstrDecls(cndecls);
    else if(cndecls.length==1) writeJConstrDecl(cndecls[0]);
        
    // Append methods
    JMethodDecl mdecls[] = aCDecl.getMethodDecls(), mlast = mdecls.length>0? mdecls[mdecls.length-1] : null;
    Set <String> mdone = new HashSet();
    for(JMethodDecl md : mdecls) { String name = md.getName();
        if(mdone.contains(name)) continue;
        JMethodDecl allInst[] = aCDecl.getMethodDecls(name, false);
        if(allInst.length>1) writeJMethodDecls(allInst);
        else if(allInst.length==1) writeJMethodDecl(allInst[0]);
        JMethodDecl allStatic[] = aCDecl.getMethodDecls(name, true);
        if(allStatic.length>1) writeJMethodDecls(allStatic);
        else if(allStatic.length==1) writeJMethodDecl(allStatic[0]); if(md!=mlast) endln();
        mdone.add(name);
    }
        
    // Terminate
    outdent();
    append('}').endln().endln();
    
    // If not interface, write Class/Interfaces stuff
    if(!isInterface) {
    
        // Write class
        String cpath = aCDecl.getEvalClassName().replace('$','.');
        append(cname).append("[\"__class\"] = \"").append(cpath).append("\";").endln();
        
        // Write interfaces
        if(itypes.size()>0) {
            append(cname).append("[\"__interfaces\"] = [");
            for(JType ityp : itypes) { String cp = ityp.getEvalClassName().replace('$','.');
                append('"').append(cp).append('"'); if(ityp!=ilast) append(","); }
            append("];").endln();
        }
    }
    
    // Append inner classes
    JClassDecl cdecls[] = aCDecl.getClassDecls();
    if(cdecls.length>0) {
        //System.out.println("Writing inner classes for " + aCDecl.getFile().getName() + " (" + cdecls.length + ')');
        endln().append("export namespace ").append(cname).append(" {").endln().endln().indent();
        for(JClassDecl cd : cdecls)
            writeJClassDecl(cd);
        outdent().append('}').endln();
    }
}

/**
 * Writes a JClassDecl of ClassType.Enum.
 */
public void writeJClassDeclEnum(JClassDecl aCDecl)
{
    // Get class name
    String cname = aCDecl.getSimpleName();
    
    // Add export call
    if(aCDecl.getParent() instanceof JClassDecl || aCDecl.getFile().getPackageName()!=null)
        append("export ");

    // Append class label with modifiers: public class/interface XXX ...
    JModifiers mods = aCDecl.getMods();
    String mstr = mods!=null && mods.isAbstract()? "abstract " : "";
    append(mstr);
    append("enum ");
    append(cname).append(' ');
    
    // Write class label close char
    append('{').endln();
    indent();
    
    // Append enum constants
    List <JEnumConst> econsts = aCDecl.getEnumConstants();
    writeJNodesJoined(econsts, ", "); endln();
        
    // Terminate
    outdent();
    append('}').endln();
}

/**
 * Write JEnumConst.
 */
public void writeJEnumConst(JEnumConst aConst)  { append(aConst.getName()); }

/**
 * Writes a JFieldDecl.
 */
public void writeJFieldDecl(JFieldDecl aFDecl)
{
    // Get modifiers and JVarDecls
    JModifiers mods = aFDecl.getMods();
    List <JVarDecl> vds = aFDecl.getVarDecls();
    
    // Iterate over VarDecls and write mods, var decl and statement/line terminator
    for(JVarDecl vd : vds) {
        writeJModifiers(mods);
        writeJVarDecl(vd);
        append(';').endln();
    }
}

/**
 * Writes a JConstrDecl.
 */
public void writeJConstrDecl(JConstrDecl aCDecl)
{
    // Write modifiers
    JModifiers mods = aCDecl.getMods();
    writeJModifiers(mods);

    // Write 'constructor' label and start char
    append("constructor(");
    
    // Write parameters
    List <JVarDecl> params = aCDecl.getParameters();
    JVarDecl last = params.size()>0? params.get(params.size()-1) : null;
    for(JVarDecl param : params) {
        writeJVarDecl(param); if(param!=last) append(", "); }
        
    // Write parameters close char
    append(") ");
    
    // Write statement block
    writeJStmtBlock(aCDecl.getBlock(), false); endln();
}

/**
 * Writes a JConstrDecl.
 */
public void writeJConstrDecls(JConstrDecl theCDecls[])
{
    // Get common return type and parameter types
    TSWriterUtils.sort(theCDecls);
    JModifiers mods = theCDecls[0].getMods();
    JVarDecl params[] = TSWriterUtils.getCommonParams(theCDecls);
    writeJMethodDeclHead(mods, "constructor", null, Arrays.asList(params), true);
    
    // Write block char, endline and indent
    append('{').endln().indent();
    
    // Write JStmtConstrCall if needed
    JConstrDecl cd0 = theCDecls[0];
    JClassDecl clsdecl = cd0.getEnclosingClassDecl();
    if(clsdecl.getSuperClass()!=Object.class)
        append("super();").endln();

    // Get dispatch conditional lines: if(arg-types-for-meth-1) return meth-1(args);
    List <String> dispatchConds = TSWriterUtils.getMethodDispatchConditionals(theCDecls, params, false);
    
    // Iterate over constructor decls and write if blocks
    for(int i=0,iMax=theCDecls.length;i<iMax;i++) { JConstrDecl cd = theCDecls[iMax-i-1];
    
        // Get dispatch conditional and append
        String dline = dispatchConds.get(i);
        append(dline).append(' ');
        
        // Write mappings for var name differences
        List <JVarDecl> cdparams = cd.getParameters();
        List <String> lvarMaps = null;
        for(int j=0,jMax=cdparams.size();j<jMax;j++) { JVarDecl vd = cdparams.get(j);
            if(!vd.getName().equals(params[j].getName())) {
                String lvar = "let " + vd.getName() + " = " + params[j].getName() + ";";
                if(lvarMaps==null) lvarMaps = new ArrayList(); lvarMaps.add(lvar);
            }
        }
        writeJStmtBlock(cd.getBlock(), false, lvarMaps);
        if(i+1<iMax) endln();
    }
    
    // outdent, write block end char and endline
    outdent().append('}').endln().endln();
}

/**
 * Writes a JMethodDecl.
 */
public void writeJMethodDecl(JMethodDecl aMDecl)
{
    // Get parts and write actual declaration line
    JModifiers mods = aMDecl.getMods();
    String name = aMDecl.getName();
    JType rtype = aMDecl.getType();
    List <JVarDecl> params = aMDecl.getParameters();
    writeJMethodDeclHead(mods, name, rtype, params, false);
    
    // If interface or abstract, just write semi-colon
    if(aMDecl.getEnclosingClassDecl().isInterface() || mods!=null && mods.isAbstract())
        append(';').endln();
    
    // Otherwise, write method block
    else writeJStmtBlock(aMDecl.getBlock(), false);
}

/**
 * Writes a JMethodDecl.
 */
public void writeJMethodDeclHead(JModifiers theMods, String aName, JType aReturnType, List <JVarDecl> theParams,
    boolean isCombined)
{
    // Write modifiers, method name and params start char
    writeJModifiers(theMods);
    append(aName).append("(");
    
    // Write params
    JVarDecl plast = theParams.size()>0? theParams.get(theParams.size()-1) : null;
    for(JVarDecl p : theParams) {
        writeJExprId(p.getId()); if(isCombined) append('?');
        append(" : ");
        String tstr = TSWriterUtils.getTypeString(p.getType());
        append(tstr); if(p!=plast) append(", ");
    }
    
    // Write params close char and return type (if not empty/void)
    append(") ");
    String tstr = aReturnType!=null? TSWriterUtils.getTypeString(aReturnType) : "";
    if(tstr.length()>0) append(": ").append(tstr).append(' ');
}
    
/**
 * Writes an array of JMethodDecl objects for the same name.
 */
public void writeJMethodDecls(JMethodDecl theMDecls[])
{
    // Get method declaration parts and write header: "public method(arg? : type, ...) : type""
    TSWriterUtils.sort(theMDecls);
    JMethodDecl md0 = theMDecls[0];
    JModifiers mods = md0.getMods();
    String name = md0.getName();
    JType rtype = TSWriterUtils.getCommonReturnType(theMDecls);
    JVarDecl params[] = TSWriterUtils.getCommonParams(theMDecls);
    writeJMethodDeclHead(mods, name, rtype, Arrays.asList(params), true);
    
    // If interface, just return
    if(md0.getEnclosingClassDecl().isInterface()) {
        append(';').endln(); return; }
    
    // Start method block and indent
    append('{').endln().indent();
    
    // Get dispatch conditional lines and append: if(arg-types-for-meth-1) return meth-1(args);
    List <String> dispatchLines = TSWriterUtils.getMethodDispatchConditionals(theMDecls, params, true);
    for(String dline : dispatchLines)
        append(dline).endln();
    
    // Outdent and end method block    
    outdent().append('}').endln().endln();
    
    // Write actual methods with new names: method_type_type(args)
    for(JMethodDecl md : theMDecls) {
        String mname = TSWriterUtils.getMethodNameUnique(md);
        writeJMethodDeclHead(md.getMods(), mname, md.getType(), md.getParameters(), false);
        writeJStmtBlock(md.getBlock(), false); endln();
    }
}

/**
 * Writes a JVarDecl.
 */
public void writeJVarDecl(JVarDecl aVD)
{
    // Write name
    JExprId name = aVD.getId();
    writeJExprId(name); append(" : ");
    
    // Write type
    JType type = aVD.getType();
    String tstr = TSWriterUtils.getTypeString(type);
    for(int i=0,iMax=aVD.getArrayCount();i<iMax;i++) tstr += "[]";
    append(tstr);
    
    // Write initializer
    JExpr init = aVD.getInitializer();
    if(init!=null) {
        append(" = ");
        writeJExpr(init);
    }
}

/**
 * Writes a JModifiers.
 */
public void writeJModifiers(JModifiers aMod)
{
    String str = getJModifierString(aMod);
    append(str);
}

/**
 * Returns a string for JModifier.
 */
public String getJModifierString(JModifiers aMod)
{
    String str = ""; if(aMod==null) return str;
    if(aMod.isPublic()) str += "public ";
    if(aMod.isStatic()) str += "static ";
    if(aMod.isAbstract()) str += "abstract ";
    return str;
}

/**
 * Writes a type.
 */
public void writeJType(JType aType)
{
    String str = TSWriterUtils.getTypeString(aType); if(str.length()==0) return;
    append(str);
}

/**
 * Writes a JStmt.
 */
public void writeJStmt(JStmt aStmt)
{
    if(aStmt instanceof JStmtAssert) writeJStmtAssert((JStmtAssert)aStmt);
    else if(aStmt instanceof JStmtBlock) writeJStmtBlock((JStmtBlock)aStmt, false);
    else if(aStmt instanceof JStmtBreak) writeJStmtBreak((JStmtBreak)aStmt);
    else if(aStmt instanceof JStmtClassDecl) writeJStmtClassDecl((JStmtClassDecl)aStmt);
    else if(aStmt instanceof JStmtConstrCall) writeJStmtConstrCall((JStmtConstrCall)aStmt);
    else if(aStmt instanceof JStmtContinue) writeJStmtContinue((JStmtContinue)aStmt);
    else if(aStmt instanceof JStmtDo) writeJStmtDo((JStmtDo)aStmt);
    else if(aStmt instanceof JStmtEmpty) writeJStmtEmpty((JStmtEmpty)aStmt);
    else if(aStmt instanceof JStmtExpr) writeJStmtExpr((JStmtExpr)aStmt);
    else if(aStmt instanceof JStmtFor) writeJStmtFor((JStmtFor)aStmt);
    else if(aStmt instanceof JStmtIf) writeJStmtIf((JStmtIf)aStmt);
    else if(aStmt instanceof JStmtLabeled) writeJStmtLabeled((JStmtLabeled)aStmt);
    else if(aStmt instanceof JStmtReturn) writeJStmtReturn((JStmtReturn)aStmt);
    else if(aStmt instanceof JStmtSwitch) writeJStmtSwitch((JStmtSwitch)aStmt);
    else if(aStmt instanceof JStmtSynchronized) writeJStmtSynchronized((JStmtSynchronized)aStmt);
    else if(aStmt instanceof JStmtThrow) writeJStmtThrow((JStmtThrow)aStmt);
    else if(aStmt instanceof JStmtTry) writeJStmtTry((JStmtTry)aStmt);
    else if(aStmt instanceof JStmtVarDecl) writeJStmtVarDecl((JStmtVarDecl)aStmt);
    else if(aStmt instanceof JStmtWhile) writeJStmtWhile((JStmtWhile)aStmt);
    else throw new RuntimeException("TSWriter.writeJStmt: Unsupported statement " + aStmt.getClass());
    //else append(aStmt.getString()).endln();
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
public void writeJStmtBlock(JStmtBlock aBlock, boolean doSemicolon)  { writeJStmtBlock(aBlock, doSemicolon, null); }

/**
 * Writes a JStmtBlock.
 */
public void writeJStmtBlock(JStmtBlock aBlock, boolean doSemicolon, List <String> extraLines)
{
    // Write start and indent
    append('{').endln().indent();
    
    // Write extra lines
    if(extraLines!=null)
        for(String line : extraLines)
            append(line).endln();

    // If owned by JConstrDecl and first line not JStmtConstrCall, add one
    writeJStmtBlockConstrCall(aBlock);
    
    // Write statements
    List <JStmt> stmts = aBlock!=null? aBlock.getStatements() : null;
    if(stmts!=null)
        for(JStmt stmt : stmts)
            writeJStmt(stmt);
        
    // Outdent and terminate
    outdent(); append(doSemicolon? "};" : "}").endln();
}

/**
 * Writes the implied JStmtConstrCall for given block, if needed.
 */
public void writeJStmtBlockConstrCall(JStmtBlock aBlock)
{
    if(aBlock==null) return;
    JNode bpar = aBlock.getParent();
    JConstrDecl cd = bpar instanceof JConstrDecl? (JConstrDecl)bpar : null; if(cd==null) return;
    List <JStmt> stmts = aBlock.getStatements();
    JStmt first = stmts!=null && stmts.size()>0? stmts.get(0) : null; if(first instanceof JStmtConstrCall) return;
    JClassDecl clsdecl = cd.getEnclosingClassDecl(); if(clsdecl.getSuperClass()==Object.class) return;
    if(clsdecl.getConstructorDecls().length!=1) return;
    append("super();").endln();
}

/**
 * Writes a JStmtBreak.
 */
public void writeJStmtBreak(JStmtBreak aStmt)
{
    append("break");
    JExpr label = aStmt.getLabel();
    if(label!=null) {
        append(' '); writeJExpr(label); }
    append(';').endln();
}

/**
 * Writes a JStmtClassDecl.
 */
public void writeJStmtClassDecl(JStmtClassDecl aStmt)
{
    JClassDecl cdecl = aStmt.getClassDecl();
    writeJClassDecl(cdecl);
}

/**
 * Writes a JStmtConstrCall.
 */
public void writeJStmtConstrCall(JStmtConstrCall aStmt)
{
    // Write label (this, super, Class.this, etc.) and open char
    List <JExprId> ids = aStmt.getIds();
    writeJNodesJoined(ids, ".");
    append('(');
    
    // Write args and close char
    List <JExpr> args = aStmt.getArgs();
    writeJNodesJoined(args, ", ");
    append(");").endln();
}

/**
 * Writes a JStmtContinue.
 */
public void writeJStmtContinue(JStmtContinue aStmt)
{
    append("continue");
    JExpr label = aStmt.getLabel();
    if(label!=null) {
        append(' '); writeJExpr(label); }
    append(';').endln();
}

/**
 * Writes a JStmtDo.
 */
public void writeJStmtDo(JStmtDo aStmt)
{
    append("do ");
    JStmt stmt = aStmt.getStatement();
    writeJStmt(stmt);
    JExpr cond = aStmt.getConditional();
    append("while(");
    writeJExpr(cond);
    append(");").endln();
}

/**
 * Writes a JStmtEmpty.
 */
public void writeJStmtEmpty(JStmtEmpty aStmt)
{
    append(';').endln();
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
    JStmt stmt = aStmt.getStatement();
    if(stmt instanceof JStmtBlock)
        writeJStmt(stmt);
    else {
        endln().indent();
        writeJStmt(stmt);
        outdent();
    }
}

/**
 * Writes a JStmtLabeled.
 */
public void writeJStmtLabeled(JStmtLabeled aStmt)
{
    JExprId label = aStmt.getLabel();
    writeJExprId(label);
    append(':');
    JStmt stmt = aStmt.getStmt();
    if(stmt!=null) {
        append(' '); writeJStmt(stmt); }
    else endln();
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
 * Writes a JStmtThrow.
 */
public void writeJStmtThrow(JStmtThrow aStmt)
{
    append("throw ");
    writeJExpr(aStmt.getExpr());
    append(';').endln();
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
 * Writes a JStmtWhile.
 */
public void writeJStmtWhile(JStmtWhile aStmt)
{
    // Write "while", conditional and statement
    append("while(");
    writeJExpr(aStmt.getConditional());
    append(") ");
    writeJStmt(aStmt.getStmt());
}

/**
 * Writes a JExpr.
 */
public void writeJExpr(JExpr aExpr)
{
    if(aExpr instanceof JExprAlloc) writeJExprAlloc((JExprAlloc)aExpr);
    else if(aExpr instanceof JExprArrayIndex) writeJExprArrayIndex((JExprArrayIndex)aExpr);
    else if(aExpr instanceof JExprChain) writeJExprChain((JExprChain)aExpr);
    else if(aExpr instanceof JExpr.CastExpr) writeJExprCast((JExpr.CastExpr)aExpr);
    else if(aExpr instanceof JExprId) writeJExprId((JExprId)aExpr);
    else if(aExpr instanceof JExpr.InstanceOfExpr) writeJExprInstanceOf((JExpr.InstanceOfExpr)aExpr);
    else if(aExpr instanceof JExprLambda) writeJExprLambda((JExprLambda)aExpr);
    else if(aExpr instanceof JExprLiteral) writeJExprLiteral((JExprLiteral)aExpr);
    else if(aExpr instanceof JExprMath) writeJExprMath((JExprMath)aExpr);
    else if(aExpr instanceof JExprMethodCall) writeJExprMethodCall((JExprMethodCall)aExpr);
    else if(aExpr instanceof JExprMethodRef) writeJExprMethodRef((JExprMethodRef)aExpr);
    else if(aExpr instanceof JExprType) writeJExprType((JExprType)aExpr);
    else throw new RuntimeException("TSWriter.writeJExpr: Unsupported expression " + aExpr.getClass());
    //else append(aExpr.getString());
}

/**
 * Writes a JExprAlloc.
 */
public void writeJExprAlloc(JExprAlloc aExpr)
{
    // Get type - if array, handle separate
    JType typ = aExpr.getType();
    if(typ.isArrayType()) {
        
        // Append 'new Array(', the dimension expression (if set) and dimension close char
        JExpr dim = aExpr.getArrayDims();
        if(dim!=null) {
            append("new Array("); writeJExpr(dim); append(')'); }
        
        // If array init expresions are set, append them
        List <JExpr> inits = aExpr.getArrayInits();
        if(inits.size()>0) {
            append("[ "); writeJNodesJoined(inits, ", "); append(" ]"); }
        return;
    }
        
    // Append 'new ', type and parameter list start char
    append("new ");
    writeJType(typ);
    append('(');

    // Append args
    List <JExpr> args = aExpr.getArgs(); JExpr last = args.size()>0? args.get(args.size()-1) : null;
    for(JExpr arg : aExpr.getArgs()) {
        writeJExpr(arg); if(arg!=last) append(", "); }
        
    // Append close char
    append(')');
    
    // Append ClassDecl
    if(aExpr.getClassDecl()!=null)
        System.err.println("Need to write ClassDecl for " + aExpr.getClassDecl().getEvalClassName());
}

/**
 * Writes a JExprArrayIndex.
 */
public void writeJExprArrayIndex(JExprArrayIndex aExpr)
{
    JExpr expr = aExpr.getArrayExpr();
    writeJExpr(expr);
    JExpr iexpr = aExpr.getIndexExpr();
    append('['); writeJExpr(iexpr); append(']');
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
 * Writes a JExprChain.
 */
public void writeJExprChain(JExprChain aExpr)
{
    // Write component expressions
    List <JExpr> exprs = aExpr.getExpressions(); JExpr last = exprs.get(exprs.size()-1);
    for(JExpr exp : exprs) {
        writeJExpr(exp); if(exp!=last) append('.'); }
}

/**
 * Writes a JExprId.
 */
public void writeJExprId(JExprId aExpr)
{
    // If Enum class id append enclosing class
    if(aExpr.isEnumId() && aExpr.getParentExpr()==null) { JavaDecl decl = aExpr.getDecl();
        Class cls = aExpr.getEvalClass(), ecls = cls!=null? cls.getEnclosingClass() : null;
        if(ecls!=null) append(ecls.getSimpleName()).append('.');
    }
    
    // If Enum constant id append class and enclosing class and parent
    else if(aExpr.isEnumConstId() && aExpr.getParentExpr()==null) { JavaDecl decl = aExpr.getDecl();
        Class cls = aExpr.getEvalClass(), ecls = cls!=null? cls.getEnclosingClass() : null;
        if(ecls!=null) append(ecls.getSimpleName()).append('.');
        if(cls!=null) append(cls.getSimpleName()).append('.');
    }
    
    // If id is field or method reference and orphan, append "this." (or simple class name if static)
    else if(aExpr.isFieldRef() && aExpr.getParentExpr()==null) { JavaDecl decl = aExpr.getDecl();
        if(decl.isStatic()) {
            if(!decl.getClassName().startsWith("jsweet.")) append(decl.getClassSimpleName()).append('.'); }
        else append("this.");
    }
    else if(aExpr.isMethodCall() && aExpr.getMethodCall().getParentExpr()==null) { JavaDecl decl = aExpr.getDecl();
        if(decl.isStatic()) append(decl.getClassSimpleName()).append('.');
        else append("this.");
    }
    
    // Append id
    String str = aExpr.getName();
    if(str.equals("in")) str = "__in";
    else if(str.equals("function")) str = "__function";
    append(str);
}

/**
 * Writes a JExpr.InstanceOfExpr.
 */
public void writeJExprInstanceOf(JExpr.InstanceOfExpr aExpr)
{
    // Get the type and type string
    JExpr expr = aExpr.getExpr();
    JType typ = aExpr.getType();
    String tstr = TSWriterUtils.getTypeString(typ, false);
    
    // Handle primitive types
    if(tstr.equals("number") || tstr.equals("string") || tstr.equals("boolean")) {
        append("typeof "); writeJExpr(expr); append(" === \'").append(tstr).append('\'');
        return;
    }
    
    // If array type, replace with Array
    if(typ.isArrayType())
        tstr = "Array";
    
    // Get expr and write: expr!=null && expr instanceof
    writeJExpr(expr); append("!=null && ");
    writeJExpr(expr); append(" instanceof "); append(tstr);
}

/**
 * Writes a JExprLambda.
 */
public void writeJExprLambda(JExprLambda aExpr)
{
    // Write parameters
    append('(');
    List <JVarDecl> params = aExpr.getParams(); JVarDecl last = params.size()>0? params.get(params.size()-1) : null;
    for(JVarDecl param : aExpr.getParams()) {
        append(param.getName()); if(param!=last) append(','); }
    append(')');
    
    // Write fat arrow
    append(" => ");
    
    // Write expression or statement block
    if(aExpr.getExpr()!=null)
        writeJExpr(aExpr.getExpr());
    else writeJStmtBlock(aExpr.getBlock(), false);
}

/**
 * Writes a JExprLiteral.
 */
public void writeJExprLiteral(JExprLiteral aExpr)
{
    String str = aExpr.getValueString();
    JExprLiteral.LiteralType typ = aExpr.getLiteralType();
    if(typ==JExprLiteral.LiteralType.Long || typ==JExprLiteral.LiteralType.Float ||
        typ==JExprLiteral.LiteralType.Double) {
        char c = str.charAt(str.length()-1);
        if(Character.isLetter(c)) str = str.substring(0, str.length()-1);
    }
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
    
    // Handle unary pre
    else if(op==JExprMath.Op.Negate || op==JExprMath.Op.Not ||
        op==JExprMath.Op.PreDecrement || op==JExprMath.Op.PreIncrement) {
        JExpr exp0 = aExpr.getOperand(0);
        append(JExprMath.getOpString(op));
        writeJExpr(exp0);
    }
    
    // Handle unary post
    else if(op==JExprMath.Op.PostDecrement || op==JExprMath.Op.PostIncrement) {
        JExpr exp0 = aExpr.getOperand(0);
        append(JExprMath.getOpString(op));
        writeJExpr(exp0);
    }
    
    else throw new RuntimeException("TSWriter.writeJExprMath: Unsupported op: " + op);
}

/**
 * Writes a JExprMethodCall.
 */
public void writeJExprMethodCall(JExprMethodCall aExpr)
{
    // If special, just return
    if(writeJExprMethodCallSpecial(aExpr)) return;
    
    // Append name and open char
    JExprId id = aExpr.getId(); String name = id.getName();
    writeJExprId(id); append('(');
    
    // Append args and close char
    List <JExpr> args = aExpr.getArgs();
    writeJNodesJoined(args, ", ");
    append(')');
}

/**
 * Writes a JExprMethodCall.
 */
public boolean writeJExprMethodCallSpecial(JExprMethodCall aExpr)
{
    JExprId id = aExpr.getId(); String name = id.getName();
    
    // Handle jsweet.util.Globals.union()
    if(name.equals("union")) {
        JavaDecl decl = id.getDecl(); if(decl==null || !decl.getClassName().startsWith("jsweet.")) return false;
        JExpr arg = aExpr.getArgs().get(0);
        append("(<any>"); writeJExpr(arg); append(')');
        return true;
    }
    
    // Handle jsweet.util.Globals.function()
    if(name.equals("function")) {
        JavaDecl decl = id.getDecl(); if(decl==null || !decl.getClassName().startsWith("jsweet.")) return false;
        JExpr arg = aExpr.getArgs().get(0);
        writeJExpr(arg);
        return true;
    }
    
    // Handle Arrays.copyOf(array,int,Class): Drop last param
    if(name.equals("copyOf")) {
        
        // If not 3 args or last arg not class or method not from java.util.Arrays, just return
        if(aExpr.getArgCount()!=3 || !(aExpr.getArg(2).getEvalClass() instanceof Class)) return false;
        JavaDecl decl = id.getDecl();if(decl==null || !decl.getClassName().startsWith("java.util.Arrays")) return false;
    
        // Write method with only two args
        writeJExprId(id); append('(');
        JExpr arg0 = aExpr.getArg(0), arg1 = aExpr.getArg(1);
        writeJExpr(arg0); append(", "); writeJExpr(arg1); append(')');
        return true;
    }
    
    return false;
}

/**
 * Writes a JExprMethodRef.
 */
public void writeJExprMethodRef(JExprMethodRef aExpr)
{
    System.out.println("TSWriter: Need to write method ref: " + aExpr.getFile().getEvalClassName());
}

/**
 * Writes a JExprType.
 */
public void writeJExprType(JExprType aExpr)
{
    JType typ = aExpr.getType();
    writeJType(typ);
}

/**
 * Append String.
 */
public TSWriter append(String aStr)  { cd(); _sb.append(aStr); return this; }

/**
 * Append char.
 */
public TSWriter append(char aValue)  { cd(); _sb.append(aValue); return this; }

/**
 * Append Int.
 */
public TSWriter append(int aValue)  { cd(); _sb.append(aValue); return this; }

/**
 * Append Double.
 */
public TSWriter append(double aValue)  { cd(); _sb.append(aValue); return this; }

/**
 * Append newline.
 */
public TSWriter endln()  { _sb.append('\n'); _lineStart = true; return this; }

/**
 * Append indent.
 */
public TSWriter indent()  { _indent++; return this; }

/**
 * Append indent.
 */
public TSWriter outdent()  { _indent--; return this; }

/**
 * Append indent.
 */
public TSWriter appendIndent()  { for(int i=0;i<_indent;i++) _sb.append(_indentStr); return this; }

/**
 * Checks for indent.
 */
protected void cd()  { if(_lineStart) appendIndent(); _lineStart = false; }

}