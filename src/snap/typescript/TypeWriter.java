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
    String iname = anImp.getName();
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
    String mstr = mods.isPublic() && mods.isStatic()? "public static " : mods.isPublic()? "public " : "";

    // Write modifiers, method name and args start char
    append(mstr).append(aMDecl.getName()).append("(");
    
    // Write parameters
    List <JVarDecl> params = aMDecl.getParameters();
    JVarDecl last = params.size()>0? params.get(params.size()-1) : null;
    for(JVarDecl param : aMDecl.getParameters()) {
        writeJVarDecl(param); if(param!=last) append(", "); }
        
    // Write return type
    append(") : ").append(aMDecl.getType().getName()).append(' ');
    
    // Write method block
    writeJStmtBlock(aMDecl.getBlock());
}

/**
 * Writes a JVarDecl.
 */
public void writeJVarDecl(JVarDecl aVD)
{
    append(aVD.getName()).append(" : ").append(aVD.getType().getName());
}

/**
 * Writes a JStmtBlock.
 */
public void writeJStmtBlock(JStmtBlock aBlock)
{
    // Write start and indent
    append('{').endln();
    indent();
    
    // Write statements
    for(JStmt stmt : aBlock.getStatements())
        writeJStmt(stmt);
        
    // Outdent and terminate
    outdent(); append('}').endln();
}

/**
 * Writes a JStmt.
 */
public void writeJStmt(JStmt aStmt)
{
    append(aStmt.getString()).endln();
}

}