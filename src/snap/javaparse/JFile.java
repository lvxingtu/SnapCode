/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snap.javaparse;
import java.lang.reflect.*;
import java.util.*;
import snap.project.*;
import snap.util.ClassUtils;
import snap.web.WebFile;

/**
 * The top level Java part describing a Java file.
 */
public class JFile extends JNode {

    // The source file for this JFile
    WebFile                  _sourceFile;
    
    // The project for source file
    Project                  _proj;

    // The package declaration
    JPackageDecl             _packageDecl;
    
    // The list of imports
    List <JImportDecl>       _importDecls = new ArrayList();
    
    // The list of class declarations
    List <JClassDecl>        _classDecls = new ArrayList();
    
    // The parse exception, if one was hit
    Exception                _exception;
    
    // A set to hold unused imports
    Set <JImportDecl>        _unusedImports;

/**
 * Returns the WebFile for this JFile.
 */
public WebFile getSourceFile()  { return _sourceFile; }

/**
 * Sets the WebFile for this JFile.
 */
public void setSourceFile(WebFile aFile)  { _sourceFile = aFile; _proj = Project.get(aFile); }

/**
 * Returns the class loader used to resolve classes.
 */
public ClassLoader getClassLoader()  { return _proj!=null? _proj.getClassLoader() : getClass().getClassLoader(); }

/**
 * Returns the package declaration.
 */
public JPackageDecl getPackageDecl()  { return _packageDecl; }

/**
 * Sets the package declaration.
 */
public void setPackageDecl(JPackageDecl aPD)  { replaceChild(_packageDecl, _packageDecl = aPD); }

/**
 * Returns the package name.
 */
public String getPackageName()  { return _packageDecl!=null? _packageDecl.getName() : ""; }

/**
 * Returns the import statements.
 */
public List <JImportDecl> getImportDecls()  { return _importDecls; }

/**
 * Adds an import declaration.
 */
public void addImportDecl(JImportDecl anID)  { _importDecls.add(anID); addChild(anID, -1); }

/**
 * Returns the JClassDecl for the file.
 */
public JClassDecl getClassDecl()  { return _classDecls.size()>0? _classDecls.get(0) : null; }

/**
 * Returns the JClassDecls for the file.
 */
public List <JClassDecl> getClassDecls()  { return _classDecls; }

/**
 * Adds a JClassDecls for the file.
 */
public void addClassDecl(JClassDecl aCD)  { _classDecls.add(aCD); addChild(aCD, -1); }

/**
 * Override to return this file node.
 */
public JFile getFile()  { return this; }

/**
 * Override to get name from ClassDecl.
 */
protected String getNameImpl()
{
    JClassDecl cd = getClassDecl(); if(cd==null) return "NoClassDefined";
    String cname = cd.getSimpleName(), pname = getPackageName();
    return pname!=null && pname.length()>0? pname + '.' + cname : cname;
}

/**
 * Returns the type class of this file.
 */
protected JavaDecl getDeclImpl()  { JClassDecl cd = getClassDecl(); return cd!=null? cd.getDecl() : null; }

/**
 * Override to check for package name, import class name, static import class member.
 */
@Override
protected JavaDecl resolveName(JNode aNode)
{
    // Get node info
    String name = aNode.getName();
    
    // If it's in JPackageDecl, it's a Package
    if(isKnownPackageName(name))
        return new JavaDecl(JavaDecl.Type.Package, name);
    
    // See if it's a known class name using imports
    String cname = getImportClassName(name);
    Class cls = cname!=null? getClassForName(cname) : null;
    if(cls!=null)
        return new JavaDecl(cls);
        
    // See if it's a known static import class member
    Field field = (Field)getImportClassMember(name, null);
    if(field!=null)
        return new JavaDecl(field);

    // Do normal version
    return super.resolveName(aNode);
}

/**
 * Returns an import that can be used to resolve the given name.
 */
public JImportDecl getImport(String aName)
{
    // Handle fully specified name
    if(isKnownClassName(aName)) return null;
    
    // Iterate over imports to see if any can resolve name
    JImportDecl match = null;
    for(int i=_importDecls.size()-1; i>=0; i--) { JImportDecl imp = _importDecls.get(i);
    
        // Get import name (just continue if null)
        String iname = imp.getName(); if(iname==null) continue;
        
        // If import is static, see if it matches given name
        if(imp.isStatic() && iname.endsWith(aName)) {
            if(iname.length()==aName.length() || iname.charAt(iname.length()-aName.length()-1)=='.') {
                match = imp; break; }}
    
        // If import is inclusive ("import xxx.*") and ImportName.aName is known class, return class name
        else if(imp.isInclusive && match==null) { String cname = iname + '.' + aName;
            if(isKnownClassName(cname) || imp.isClassName() && isKnownClassName(iname + '$' + aName))
                match = imp; }
        
        // Otherwise, see if import refers explicitly to class name
        else if(iname.endsWith(aName)) {
            if(iname.length()==aName.length() || iname.charAt(iname.length()-aName.length()-1)=='.') {
                match = imp; break; }}
    }
    
    // Remove match from UnusedImports and return
    if(match!=null) {
        if(match.isInclusive()) match.addFoundClassName(aName);
        match._used = true;
    }
    return match;
}

/**
 * Returns an import that can be used to resolve the given name.
 */
public JImportDecl getStaticImport(String aName, Class theParams[])
{
    // Iterate over imports to see if any can resolve name
    for(int i=_importDecls.size()-1; i>=0; i--) { JImportDecl imp = _importDecls.get(i);
    
        // If import is static ("import static xxx.*") and name/params is known field/method, return member
        if(imp.isStatic()) {
            Member mbr = imp.getImportMember(aName, theParams);
            if(mbr!=null) {
                _unusedImports.remove(imp); return imp; }
        }
    }
    
    // Return null since import not found
    return null;
}

/**
 * Returns a Class name for given name referenced in file.
 */
public String getImportClassName(String aName)
{
    // Handle fully specified name
    if(isKnownClassName(aName))
        return aName;
    
    // If name has parts, handle them separately
    if(aName.indexOf('.')>0) {
        String names[] = aName.split("\\.");
        String cname = getImportClassName(names[0]); if(cname==null) return null;
        Class cls = getClassForName(cname);
        for(int i=1;cls!=null && i<names.length;i++)
            cls = ClassUtils.getClass(cls, names[i]);
        return cls!=null? cls.getName() : null;
    }
    
    // Get import for name
    JImportDecl imp = getImport(aName);
    if(imp!=null)
        return imp.getImportClassName(aName);
    
    // If file declares package, see if it's in package
    if(getPackageName().length()>0) { String cname = getPackageName() + '.' + aName;
        if(isKnownClassName(cname))
            return cname; }
    
    // Try "java.lang" + name
    try { return Class.forName("java.lang." + aName).getName(); }
    catch(ClassNotFoundException e) { }
    
    // Return null since class not found
    return null;
}

/**
 * Returns a Class name for given name referenced in file.
 */
public Member getImportClassMember(String aName, Class theParams[])
{
    JImportDecl imp = getStaticImport(aName, theParams);
    if(imp!=null)
        return imp.getImportMember(aName, theParams);
    return null;
}

/**
 * Returns unused imports for file.
 */
public Set <JImportDecl> getUnusedImports()
{
    if(_unusedImports!=null) return _unusedImports;
    resolveClassNames(this);
    Set <JImportDecl> uimps = new HashSet();
    for(JImportDecl imp : getImportDecls()) if(!imp._used) uimps.add(imp);
    System.out.println("Expanded imports in file " + getClassName() + ":");
    for(JImportDecl imp : getImportDecls()) {
        if(imp.isInclusive() && !imp.isStatic() && imp.getFoundClassNames().size()>0) {
            System.out.print("    " + imp.getString() + ": ");
            List <String> names = imp.getFoundClassNames(); String last = names.size()>0? names.get(names.size()-1):null;
            for(String n : names) {
                System.out.print(n); if(n!=last) System.out.print(", "); else System.out.println(); }
        }
    }
    return _unusedImports = uimps;
}

/**
 * Forces all nodes to resolve class names.
 */
private void resolveClassNames(JNode aNode)
{
    // Handle JType
    if(aNode instanceof JType || aNode instanceof JExprId)
        aNode.getClassName();
        
    // Recurse for children
    for(JNode child : aNode.getChildren())
        resolveClassNames(child);
}

/**
 * Returns the exception if one was hit.
 */
public Exception getException()  { return _exception; }

/**
 * Sets the exception.
 */
public void setException(Exception anException)  { _exception = anException; }

/**
 * Init from another JFile.
 */
protected void init(JFile aJFile)
{
    _name = aJFile._name;
    _startToken = aJFile._startToken; _endToken = aJFile._endToken;
    _children = aJFile._children; for(JNode c : _children) c._parent = this;

    _sourceFile = aJFile._sourceFile; _proj = aJFile._proj; _packageDecl = aJFile._packageDecl;
    _importDecls = aJFile._importDecls; _classDecls = aJFile._classDecls; _exception = aJFile._exception;
}

}