package javakit.parse;
import java.util.*;
import snap.parse.*;
import snap.project.*;
import snap.web.WebFile;

/**
 * A file object for managing Java files.
 */
public class JavaData {

    // The java file
    WebFile          _file;
    
    // The Project that owns this file
    Project          _proj;

    // The set of declarations in this JavaFile
    Set <JavaDecl>   _decls = new HashSet();
    
    // The set of references in this JavaFile
    Set <JavaDecl>   _refs = new HashSet();

    // The set of files that our file depends on
    Set <WebFile>    _dependencies = new HashSet();
    
    // The set of files that depend on our file
    Set <WebFile>    _dependents = new HashSet();

    // The parsed version of this JavaFile
    JFile            _jfile;
    
/**
 * Creates a new JavaData for given file.
 */
public JavaData(WebFile aFile)  { _file = aFile; }

/**
 * Returns the project for this JavaFile.
 */
public Project getProject()  { return _proj!=null? _proj : (_proj=Project.get(_file)); }

/**
 * Returns the class files for this JavaFile.
 */
public WebFile[] getClassFiles()
{
    Project proj = getProject();
    WebFile cfiles[] = proj.getClassFiles(_file);
    return cfiles;
}

/**
 * Returns the declarations in this JavaFile.
 */
public synchronized Set <JavaDecl> getDecls()
{
    // If already loaded, just return
    if(_decls.size()>0) return _decls;
    
    // Iterate over JavaFile.Class files
    Project proj = getProject();
    WebFile cfiles[] = getClassFiles();
    for(WebFile cfile : cfiles) {
        String cname = proj.getClassName(cfile);
        JavaDeclClass cdecl = proj.getClassDecl(cname);
        if(cdecl==null) { System.err.println("JavaData.getDecls: Can't find decl " + cname); continue; }
        _decls.addAll(cdecl.getAllDecls());
    }
    
    // Return decls
    return _decls;
}

/**
 * Returns the references in this JavaFile.
 */
public Set <JavaDecl> getRefs()  { return _refs; }
    
/**
 * Returns the set of files that our file depends on.
 */
public Set <WebFile> getDependencies()  { return _dependencies; }

/**
 * Returns the set of files that depend on our file.
 */
public Set <WebFile> getDependents()  { return _dependents; }

/**
 * Returns whether dependencies are set.
 */
public boolean isDependenciesSet()  { return _dset; } boolean _dset;

/**
 * Updates dependencies for a given file and list of new/old dependencies.
 * 
 * @return whether any dependencies (the declarations or references) have changed since last update.
 */
public synchronized boolean updateDependencies()
{
    // Get Java file, project, RootProject, ProjectSet and class files
    WebFile jfile = _file;
    Project proj = getProject();
    WebFile cfiles[] = getClassFiles();
    
    // Get new declarations
    boolean declsChanged = false;
    if(cfiles!=null) for(WebFile cfile : cfiles) {
        String cname = proj.getClassName(cfile);
        JavaDeclClass cdecl = proj.getClassDecl(cname); if(cdecl==null) return false;
        try { declsChanged = declsChanged || cdecl.updateDecls(); }
        catch(Throwable t) { System.err.printf("JavaData.updateDepends failed to get decls in %s: %s\n", cfile, t); }
    }
    
    // If declarations have changed, clear cached list
    if(declsChanged)
        _decls.clear();
    
    // Get new refs
    Set <JavaDecl> nrefs = new HashSet(); _dset = true; _jfile = null;
    if(cfiles!=null) for(WebFile cfile : cfiles) {
        ClassData cdata = ClassData.get(cfile);
        try { cdata.getRefs(nrefs); }
        catch(Throwable t) { System.err.printf("JavaData.updateDepends failed to get refs in %s: %s\n", cfile, t); }
    }
    
    // If references haven't changed, just return
    if(nrefs.equals(_refs))
        return declsChanged;
    
    // Get set of added/removed refs
    Set <JavaDecl> refsAdded = new HashSet(_refs); refsAdded.addAll(nrefs);
    Set <JavaDecl> refsRemoved = new HashSet(refsAdded); refsRemoved.removeAll(nrefs); refsAdded.removeAll(_refs);
    _refs = nrefs;
    
    // Iterate over added refs and add dependencies
    Project rootProj = proj.getRootProject();
    ProjectSet projSet = rootProj.getProjectSet();
    for(JavaDecl ref : refsAdded) {
        if(!ref.isClass()) continue;
        String cname = ref.getRootClassName();
        if(cname.startsWith("java") && (cname.startsWith("java.") || cname.startsWith("javax.") ||
            cname.startsWith("javafx"))) continue;
        WebFile file = projSet.getJavaFile(cname);
        if(file!=null && file!=jfile && !_dependencies.contains(file)) {
            _dependencies.add(file);
            JavaData.get(file)._dependents.add(jfile);
        }
    }
    
    // Iterate over removed refs and add dependencies
    for(JavaDecl ref : refsRemoved) {
        if(!ref.isClass()) continue;
        String cname = ref.getRootClassName();
        WebFile file = projSet.getJavaFile(cname);
        if(file!=null && _dependencies.contains(file)) {
            _dependencies.remove(file);
            JavaData.get(file)._dependents.remove(jfile);
        }
    }
    
    // Return true since references changed
    return true;
}

/**
 * Removes dependencies.
 */
public void removeDependencies()
{
    for(WebFile dep : _dependencies) JavaData.get(dep)._dependents.remove(_file);
    _dependencies.clear(); _decls.clear(); _refs.clear(); _dset = false;
}

/**
 * Returns the parsed Java file.
 */
public JFile getJFile()  { return _jfile!=null? _jfile : (_jfile=createJFile()); }

/**
 * Returns the parsed Java file.
 */
protected JFile createJFile()
{
    // Get Java string and parser and generate JavaFile
    String string = _file.getText();
    JavaParser javaParser = JavaParser.getShared();
    JFile jfile = javaParser.getJavaFile(string);
    jfile.setSourceFile(_file);
    return jfile;
}

/**
 * Returns a set of unused imports.
 */
public List <BuildIssue> getUnusedImports()
{
    String string = _file.getText();
    Parser ip = JavaParser.getShared().getImportsParser();
    ParseNode node = null; try { node = string!=null && string.length()>0? ip.parse(string) : null; }
    catch(ParseException e) { System.err.println("JavaData.getUnusedImports Parse Exception"); e.printStackTrace(); }
    if(node==null) return Collections.EMPTY_LIST;
    
    // Set JFile
    JFile jfile = node!=null? node.getCustomNode(JFile.class) : new JFile();
    jfile.setSourceFile(_file);
    List <JImportDecl> imports = jfile.getImportDecls(); if(imports.size()==0) return Collections.EMPTY_LIST;
    Set <JImportDecl> iset = new HashSet(imports);
    
    // Iterate over class references and remove used ones
    Set <JavaDecl> refs = getRefs();
    for(JavaDecl ref : refs) {
        if(!ref.isClass()) continue;
        String sname = ref.getSimpleName();
        JImportDecl idecl = jfile.getImport(sname);
        if(idecl!=null) {
            iset.remove(idecl); if(iset.size()==0) return Collections.EMPTY_LIST; }
    }
    
    // Iterate over references and remove used ones
    /*for(JavaDecl ref : refs) { if(ref.isClass() || ref.isConstructor()) continue;
        String sname = ref.getEvalType().getSimpleName(); JImportDecl idecl = jfile.getImport(sname);
        if(idecl!=null) { iset.remove(idecl); if(iset.size()==0) return Collections.EMPTY_LIST; } }*/
    
    // Do full parse and eval
    iset = getJFile().getUnusedImports();
    if(iset.size()==0) return Collections.EMPTY_LIST;
    
    // Create BuildIssues and return
    List <BuildIssue> issues = new ArrayList();
    for(JImportDecl idecl : iset)
        issues.add(new BuildIssue().init(_file, BuildIssue.Kind.Warning,
            "The import " + idecl.getName() + " is never used",
            idecl.getLineIndex(), 0, idecl.getStart(), idecl.getEnd()));
    return issues;
}

/**
 * Returns the JavaData for given file.
 */
public static JavaData get(WebFile aFile)
{
    JavaData data = (JavaData)aFile.getProp(JavaData.class.getName());
    if(data==null) aFile.setProp(JavaData.class.getName(), data = new JavaData(aFile));
    return data;
}

}