package snap.typescript;
import java.util.List;
import snap.javaparse.*;

/**
 * A class to convert Java to TypeScript.
 */
public class TypeWriter {

    // The TypeBuffer
    TypeBuffer  _tb = new TypeBuffer();

/**
 * Returns the string for JNode.
 */
public String getString(JNode aNode)
{
    writeJNode(aNode);
    return _tb._sb.toString();
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
    _tb.append("/* Generated from Java with SnapCode - http://www.reportmill.com */\n");
    
    String pname = aJFile.getPackageName();
    if(pname!=null) _tb.append("namespace ").append(pname).append(' ').append("{\n");
    
    // Append imports
    for(JImportDecl imp : aJFile.getImportDecls())
        writeJImportDecl(imp);
    _tb.endln();
    
    
    // Append class decls
    writeJClassDecl(aJFile.getClassDecl());
}

/**
 * Write a JImportDecl.
 */
public void writeJImportDecl(JImportDecl anImp)
{
    String iname = anImp.getName();
    int ind = iname.lastIndexOf('.');
    String iname2 = anImp.isInclusive() || ind<0? iname.replace('.', '_') : iname.substring(ind+1);
    _tb.indent().append("import ").append(iname2).append(" = ").append(iname).append(";\n");
}

/**
 * Writes a JClassDecl.
 */
public void writeJClassDecl(JClassDecl aCDecl)
{
    // Append class
    _tb.indent().append("export class ").append(aCDecl.getSimpleName()).append(" {\n\n");
    
    // Append ivars
    for(JFieldDecl fd : aCDecl.getFieldDecls())
        writeJFieldDecl(fd);
        
    // Append methods
    for(JMethodDecl md : aCDecl.getMethodDecls())
        writeJMethodDecl(md);
        
    // Write class
    _tb.endln().indent();
    _tb.append(aCDecl.getSimpleName()).append("[\"__class] = \"").append(aCDecl.getClassName()).append("\";\n\n");
        
    // Terminate
    _tb.append("}");
    
    // Write main method
    _tb.endln().endln().append(aCDecl.getClassName()).append(".main(null);\n");
}

/**
 * Writes a JFieldDecl.
 */
public void writeJFieldDecl(JFieldDecl aFDecl)
{
    String mods = "public ";

    _tb.indent();
    writeJVarDecl(aFDecl.getVarDecls().get(0));
    _tb.append(";\n\n");
}

/**
 * Writes a JMethodDecl.
 */
public void writeJMethodDecl(JMethodDecl aMDecl)
{
    String mods = "public ";

    _tb.indent().append(mods).append(aMDecl.getName()).append("(");
    List <JVarDecl> params = aMDecl.getParameters();
    JVarDecl last = params.size()>0? params.get(params.size()-1) : null;
    for(JVarDecl param : aMDecl.getParameters()) {
        writeJVarDecl(param); if(param!=last) _tb.append(", "); }
    _tb.append(") : ").append(aMDecl.getType().getName()).append(' ');
    writeJStmtBlock(aMDecl.getBlock());
}

/**
 * Writes a JVarDecl.
 */
public void writeJVarDecl(JVarDecl aVD)
{
    _tb.append(aVD.getName()).append(" : ").append(aVD.getType().getName());
}

/**
 * Writes a JStmtBlock.
 */
public void writeJStmtBlock(JStmtBlock aBlock)
{
    _tb.append("{\n");
    for(JStmt stmt : aBlock.getStatements())
        writeJStmt(stmt);
    _tb.indent().append("}\n");
}

/**
 * Writes a JStmt.
 */
public void writeJStmt(JStmt aStmt)
{
    _tb.indent().indent().append(aStmt.getString()).append("\n");
}

}