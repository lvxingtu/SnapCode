/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snap.project;
import java.io.*;
import java.net.URI;
import java.util.*;
import javax.tools.*;
import javax.tools.JavaFileObject.Kind;
import snap.web.WebFile;

/**
 * A Java File Manager class for Snap.
 */
public class SnapCompilerFM extends ForwardingJavaFileManager<JavaFileManager> {

    // The SnapCompiler
    SnapCompiler              _compiler;

    // The project
    Project                   _proj;
    
    // A map of previously accessed SnapFileObjects for paths
    Map <String,SnapFileJFO>  _jfos = new HashMap();
    
    // A map cache of package files
    Map <String,Iterable<JavaFileObject>>  _pkgFiles = new HashMap();
    
    // Bogus flag to indicate we should use JavaFiles instead of ClassFiles when available
    Set <WebFile>            _buildFiles = new HashSet();
    
/**
 * Construct a new FileManager which forwards to the <var>fileManager</var>
 * for source and to the <var>classLoader</var> for classes
 */
public SnapCompilerFM(SnapCompiler aCompiler, JavaFileManager aFileManager)
{
    super(aFileManager); _compiler = aCompiler; _proj = _compiler._proj;
}

/** Override to return class loader. */
public ClassLoader getClassLoader(Location aLoc)  { return _cldr!=null? _cldr : (_cldr=_proj.createLibClassLoader()); }
ClassLoader _cldr;

/**
 * Return a FileObject for a given location from which compiler can obtain source or byte code.
 */
public FileObject getFileForInput(Location aLoc, String aPkgName, String aRelName) throws IOException
{
    System.err.println("SnapCompilerFM:getFileForInput: " + aPkgName + "." + aRelName + ", loc: " + aLoc.getName());
    //FileObject o = _fileObjects.get(getURI(location, packageName, relativeName)); if(o!=null) return o;
    return super.getFileForInput(aLoc, aPkgName, aRelName);
}

/**
 * Return a FileObject for a given location from which compiler can obtain source or byte code.
 */
public JavaFileObject getJavaFileForInput(Location aLoc, String aClassName, Kind aKind) throws IOException
{
    System.err.println("getJavaFileForInput: " + aClassName + ", kind: " + aKind);
    String cpath = _proj.getSourceDir().getDirPath() + aClassName.replace('.', '/') + ".java";
    return getJFO(cpath, null);
}

/**
 * Create a JavaFileObject for an output class file and store it in the classloader.
 */
public JavaFileObject getJavaFileForOutput(Location aLoc, String aClassName, Kind kind, FileObject aSblg)
{
    WebFile jfile = ((SnapFileJFO)aSblg).getFile();
    String cpath = "/" + aClassName.replace('.', '/') + ".class";
    WebFile cfile = _proj.getBuildFile(cpath, true, false);
    SnapFileJFO jfo = getJFO(cfile.getPath(), cfile);
    jfo._sourceFile = jfile;
    return jfo;
}

/**
 * Returns whether we have location.
 */
public boolean hasLocation(Location aLoc)
{
    if(aLoc==StandardLocation.SOURCE_PATH) return true;
    return super.hasLocation(aLoc);
}

/**
 * Override to handle JavaFileObjects (return the file's name).
 */
public String inferBinaryName(Location aLoc, JavaFileObject aFile)
{
    if(aFile instanceof SnapFileJFO) return ((SnapFileJFO)aFile).getBinaryName();
    return super.inferBinaryName(aLoc, aFile);
}

/**
 * Compare files.
 */
public boolean isSameFile(FileObject a, FileObject b)
{
    if(a==b) return true;
    //if(a instanceof SnapFileJFO || b instanceof SnapFileJFO) return false;
    return super.isSameFile(a, b);
}

/**
 * Override to return project src/bin files.
 */
public Iterable<JavaFileObject> list(Location aLoc, String aPkgName, Set<Kind> kinds, boolean doRcrs) throws IOException
{
    // Do normal version
    Iterable <JavaFileObject> iterable = super.list(aLoc, aPkgName, kinds, doRcrs);
    
    // If not CLASS_PATH or SOURCE_PATH, just return
    if(aLoc!=StandardLocation.CLASS_PATH && aLoc!=StandardLocation.SOURCE_PATH)
        return iterable;
        
    // If system path (package files were found), just return
    if(aPkgName.length()>0 && iterable.iterator().hasNext())
        return iterable;
        
    // If known system path (java., javax., etc.), just return
    if(aPkgName.startsWith("java.") || aPkgName.startsWith("javax") || aPkgName.startsWith("javafx") ||
        aPkgName.startsWith("com.sun") || aPkgName.startsWith("sun.") || aPkgName.startsWith("org.xml"))
        return iterable;
    
    // Get list from cache or load/cache from real version
    Iterable <JavaFileObject> list = _pkgFiles.get(aPkgName);
    if(list==null) _pkgFiles.put(aPkgName, list=listImpl(aPkgName, kinds));
    return list;
}

/**
 * Returns project src/bin files for package.
 */
Iterable <JavaFileObject> listImpl(String aPkgName, Set <Kind> theKinds)
{
    // Add CLASS files
    List files = new ArrayList();
    if(theKinds.contains(Kind.CLASS)) {
        WebFile pkgDir = getBuildDir(aPkgName);
        if(pkgDir!=null)
            for(WebFile file : pkgDir.getFiles()) {
                if(file.getType().equals("class")) files.add(getJFO(file.getPath(), file)); }
    }
    
    // Add Source files
    if(theKinds.contains(Kind.SOURCE)) {
        WebFile pkgDir = getSourceDir(aPkgName);
        if(pkgDir!=null)
            for(WebFile file : pkgDir.getFiles()) {
                if(isJavaFile(file)) files.add(getJFO(file.getPath(), file)); }
    }
            
    // Return files
    return files;
}

/**
 * Returns whether file is java file.
 */
boolean isJavaFile(WebFile aFile)  { return aFile.getType().equals("java"); }

/**
 * Returns the WebFile (directory) for package name build files, if available.
 */
private WebFile getBuildDir(String aPackageName)
{
    WebFile buildDir = _proj.getBuildDir();
    if(aPackageName.length()==0) return buildDir;
    String dirname = aPackageName.replace('.', '/');
    WebFile file = _proj.getBuildFile('/' + dirname, false, true); //buildDir.getFile(dirname);
    return file;
}

/**
 * Returns the WebFile (directory) for package name source files, if available.
 */
private WebFile getSourceDir(String aPackageName)
{
    WebFile sourceDir = _proj.getSourceDir();
    if(aPackageName.length()==0) return sourceDir;
    String dirname = aPackageName.replace('.', '/');
    return _proj.getSourceFile('/' + dirname, false, true); //sourceDir.getFile(dirname);
}

/**
 * Returns a JavaFleObject for given path (with option to provide file for efficiency).
 */
public synchronized SnapFileJFO getJFO(String aPath, WebFile aFile)
{
    SnapFileJFO jfo = _jfos.get(aPath);
    if(jfo==null) {
        WebFile dfile = aFile!=null? aFile : _proj.getFile(aPath);
        if(dfile!=null)
            _jfos.put(aPath, jfo = new SnapFileJFO(dfile));
    }
    return jfo;
}

/**
 * A Java File Object for a WebFile.
 */
class SnapFileJFO extends SimpleJavaFileObject {

    // The SnapCompiler and JavaFile (if available)
    WebFile         _file;
    WebFile         _sourceFile;
    boolean         _forceCompile;
    String          _bname, _str;

    /** Creates a new SnapFileJFO with WebFile, SnapCompiler and (optional) source file. */
    protected SnapFileJFO(WebFile aFile)
    {
        super(URI.create(aFile.getPath()), isJavaFile(aFile)? Kind.SOURCE : Kind.CLASS);
        _file = aFile;
        
        // If Class file, Get SourceFile and if in BuildFiles, set ForceCompile
        if(_file.getType().equals("class")) {
            _sourceFile = _proj.getJavaFile(_file); 
            if(_buildFiles.contains(_sourceFile))
                _forceCompile = true;
        }
    }
    
    /** Returns the file. */
    public WebFile getFile()  { return _file; }
    
    /** Returns ModifiedTime of WebFile. */
    public long getLastModified()  { return _forceCompile? 0 : _file.getLastModifiedTime(); }
    
    /** Returns the "binary name" for the CompilerFileManager inferBinaryName method. */
    public String getBinaryName()
    {
        if(_bname!=null) return _bname;
        String path = _file.getPath(); int index = path.lastIndexOf('.');
        String name = path.substring(1, index).replace('/', '.'); return _bname=name;
    }
    
    /** Returns the char content of file (for source file). */
    public CharSequence getCharContent(boolean ignoreEncodingErrors)
    {
        if(_str!=null) return _str;
        _str = _file.getText();
        _proj.getRootProject().getBuildIssues().remove(_file);
        return _str;
    }
    
    /** Returns input stream for source/class bytes. */
    public InputStream openInputStream()  { return _file.getInputStream(); }
        
    /** Returns output stream for byte code. */
    public OutputStream openOutputStream()
    {
        return new ByteArrayOutputStream() {
            public void close() throws IOException {
                
                // Do normal close, add SourceFile to Compiler.CompiledFiles and reset ForceCompile
                super.close();
                _compiler._compJFs.add(_sourceFile); _forceCompile = false;
                
                // Get bytes and whether class file is modified
                byte bytes[] = toByteArray();
                boolean modified = !Arrays.equals(bytes, _file.getBytes());

                // If modified, set File.Bytes and add ClassFile to ModifiedFiles and SourceFile to ModifiedSources
                if(modified) {
                    _file.setBytes(bytes);
                    _compiler._modJFs.add(_sourceFile);
                }
                
                // If file was modified or a real compile file, save
                if(modified || _file.getLastModifiedTime()<_sourceFile.getLastModifiedTime()) {
                    boolean exists = _file.getExists();
                    try { _file.save(); }
                    catch(Exception e) { throw new RuntimeException(e); }
                    if(!exists) {
                        List list = (List)_pkgFiles.get(_proj.getPackageName(_sourceFile));
                        list.add(SnapFileJFO.this);
                    }
                }
            }  
        };
    }
    
    /** This is weird, but if I don't override this, backtraces show: "MyClass from JavaCompilerFO:22" */
    public String toString()  { return uri.toString(); }
}

}