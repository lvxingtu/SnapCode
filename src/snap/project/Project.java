/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snap.project;
import java.util.*;
import snap.util.*;
import snap.web.*;

/**
 * A class to manage build attributes and behavior for a WebSite.
 */
public class Project extends SnapObject {

    // The encapsulated data site
    WebSite                            _site;
    
    // A list of build issues
    BuildIssues                        _bissues;
    
    // The last build date
    Date                               _buildDate;
    
    // The JavaFileBuilder
    JavaFileBuilder                    _javaFileBuilder = new JavaFileBuilder(this);
    
    // The default file builder
    ProjectFileBuilder                 _defaultFileBuilder = new ProjectFileBuilder.DefaultBuilder(this);
    
    // The list of Breakpoints
    Breakpoints                        _bpoints;

    // The ClassLoader for compiled class info
    ProjectClassLoader                 _clsLdr;
    
    // The project that loaded us
    Project                            _parent;
    
    // The set of projects this project depends on
    ProjectSet                         _projSet = new ProjectSet(this);
    
    // Constants for Project Settings
    public static final String         RemoteURL = "RemoteSourceURL";
    
/**
 * Creates a new Project for WebSite.
 */
protected Project(WebSite aSite)
{
    setSite(aSite);
    readSettings();
    getProjects();
}

/**
 * Returns the encapsulated WebSite.
 */
public WebSite getSite()  { return _site; }

/**
 * Sets the encapsulated WebSite.
 */
protected void setSite(WebSite aSite)  { _site = aSite; _site.setProp(Project.class.getName(), this); }

/**
 * Returns a file for given path.
 */
public WebFile getFile(String aPath)  { return _site.getFile(aPath); }

/**
 * Creates a file for given path.
 */
public WebFile createFile(String aPath, boolean isDir)  { return _site.createFile(aPath, isDir); }

/**
 * Returns the source directory.
 */
public WebFile getSourceDir()  { return getClassPath().getSourceDir(); }

/**
 * Returns the build directory.
 */
public WebFile getBuildDir()  { return getClassPath().getBuildDir(); }

/**
 * Returns the parent project for this project.
 */
public Project getParent()  { return _parent; }

/**
 * Returns the top most project.
 */
public Project getRootProject()  { return _parent!=null? _parent.getRootProject() : this; }

/**
 * Returns the list of projects this project depends on.
 */
public Project[] getProjects()  { return _projSet.getProjects(); }

/**
 * Returns the set of projects this project depends on.
 */
public ProjectSet getProjectSet()  { return _projSet; }

/**
 * Returns the source file for given path.
 */
public WebFile getSourceFile(String aPath, boolean doCreate, boolean isDir)
{
    // Look for file in source dir
    String path = aPath, spath = getSourceDir().getPath(), bpath = getBuildDir().getDirPath();
    if(bpath.length()>1 && path.startsWith(bpath)) path = path.substring(bpath.length() - 1);
    if(spath.length()>1 && !path.startsWith(spath)) path = spath + path;
    WebFile file = getSite().getFile(path);
    
    // If file still not found, maybe create and return
    if(file==null && doCreate) file = getSite().createFile(path, isDir);
    return file;
}

/**
 * Returns the build file for given path.
 */
public WebFile getBuildFile(String aPath, boolean doCreate, boolean isDir)
{
    // Look for file in build dir
    String path = aPath, spath = getSourceDir().getDirPath(), bpath = getBuildDir().getPath();
    if(spath.length()>1 && path.startsWith(spath)) path = path.substring(spath.length() - 1);
    if(bpath.length()>1 && !path.startsWith(bpath)) path = bpath + path;
    WebFile file = getBuildDir().getSite().getFile(path);
    
    // If file still not found, maybe create and return
    if(file==null && doCreate) file = getBuildDir().getSite().createFile(path, isDir);
    return file;
}

/**
 * Returns the given path stripped of SourcePath or BuildPath if file is in either.
 */
public String getSimplePath(String aPath)
{
    // Get path (make sure it's a path) and get SourcePath/BuildPath
    String path = aPath.startsWith("/")? aPath : "/" + aPath;
    String sp = getSourceDir().getPath(), bp = getBuildDir().getPath();
    
    // If in SourcePath (or is SourcePath) strip SourcePath prefix
    if(sp.length()>1 && path.startsWith(sp)) {
        if(path.length()==sp.length()) path = "/";
        else if(path.charAt(sp.length())=='/') path = path.substring(sp.length());
    }
    
    // If in BuildPath (or is BuildPath) strip BuildPath prefix
    else if(bp.length()>1 && path.startsWith(bp)) {
        if(path.length()==sp.length()) path = "/";
        else if(path.charAt(bp.length())=='/') path = path.substring(bp.length());
    }
    
    // Return path
    return path;
}

/**
 * Returns a Java file for class name.
 */
public WebFile getJavaFile(String aClassName)
{
    String cname = aClassName; int inner = cname.indexOf('$'); if(inner>0) cname = cname.substring(0, inner);
    String path = '/' + cname.replace('.', '/') + ".java";
    return getSourceFile(path, false, false);
}

/**
 * Returns the Java for a class file, if it can be found.
 */
public WebFile getJavaFile(WebFile aClassFile)
{
    String path = aClassFile.getPath().replace(".class", ".java");
    int inner = path.indexOf('$'); if(inner>0) { path = path.substring(0, inner); path += ".java"; }
    return getSourceFile(path, false, false);
}

/**
 * Returns a ClassFile for class name.
 */
public WebFile getClassFile(String aClassName)
{
    String path = '/' + aClassName.replace('.', '/') + ".class";
    return getBuildFile(path, false, false);
}

/**
 * Returns the class file for a given Java file.
 */
public WebFile getClassFile(WebFile aFile)
{
    String path = aFile.getPath().replace(".java", ".class");
    return getBuildFile(path, true, false);
}

/**
 * Returns the class files for a given Java file.
 */
public WebFile[] getClassFiles(WebFile aFile)
{
    WebFile cfile = getClassFile(aFile); if(cfile.getBytes()==null) return null;
    String cfilePrefix = cfile.getSimpleName() + '$';
    List <WebFile> files = new ArrayList(); files.add(cfile);
    for(WebFile file : cfile.getParent().getFiles())
        if(file.getType().equals("class") && file.getName().startsWith(cfilePrefix))
            files.add(file);
    return files.toArray(new WebFile[files.size()]);
}

/**
 * Returns the class name for given class file.
 */
public String getClassName(WebFile aFile)
{
    String path = aFile.getPath(); int i = path.lastIndexOf('.'); if(i>0) path = path.substring(0, i);
    return getSimplePath(path).substring(1).replace('/', '.');
}

/**
 * Returns the package name for given source file.
 */
public String getPackageName(WebFile aFile)
{
    WebFile file = aFile; while(file.isFile() || file.getName().indexOf('.')>=0) file = file.getParent();
    return getSimplePath(file.getPath()).substring(1).replace('/', '.');
}

/**
 * Returns the project class loader.
 */
public ProjectClassLoader getClassLoader()  { return _clsLdr!=null? _clsLdr : (_clsLdr=new ProjectClassLoader(this)); }

/**
 * Clears the class loader.
 */
protected void clearClassLoader()
{
    if(_clsLdr!=null) try { _clsLdr.close(); } catch(Exception e) { throw new RuntimeException(e); }
    _clsLdr = null;
}

/**
 * Returns the compiled class given file.
 */
public Class getClassForName(String aName)
{
    ClassLoader cldr = getClassLoader();
    return ClassUtils.getClass(aName, cldr);
}

/**
 * Returns the class for given file.
 */
public Class getClassForFile(WebFile aFile)
{
    String cname = getClassName(aFile);
    return getClassForName(cname);
}

/**
 * Returns the class that keeps track of class paths.
 */
public ClassPath getClassPath()  { return ClassPath.get(this); }

/**
 * Returns the paths needed to compile/run project.
 */
public String[] getClassPaths()
{
    String bpath = getClassPath().getBuildPathAbsolute();
    String libPaths[] = getLibPaths(); if(libPaths.length==0) return new String[] { bpath };
    return ArrayUtils.add(libPaths, bpath, 0);
}

/**
 * Returns the paths needed to compile/run project.
 */
public String[] getLibPaths()  { return getClassPath().getLibPathsAbsolute(); }

/**
 * Returns the breakpoints.
 */
public Breakpoints getBreakpoints()  { return _bpoints!=null? _bpoints : (_bpoints=new Breakpoints(this)); }

/**
 * Adds a Breakpoint.
 */
public void addBreakpoint(WebFile aFile, int aLine)  { getBreakpoints().addBreakpoint(aFile, aLine); }

/**
 * Reads the settings from project settings file(s).
 */
public void readSettings()  { getClassPath().readFile(); }

/**
 * Returns whether project is currently building.
 */
public boolean isBuilding()  { return _building; } boolean _building;

/**
 * Returns the last build date.
 */
public Date getBuildDate()  { return _buildDate; }

/**
 * Builds the project.
 */
public boolean buildProject(TaskMonitor aTM)
{
    // Clear classloader
    clearClassLoader();
    
    // Build files
    _building = true;
    boolean buildSuccess = _javaFileBuilder.buildFiles(aTM);
    buildSuccess |= _defaultFileBuilder.buildFiles(aTM);
    _buildDate = new Date(); _building = false;
    
    // Return build success
    return buildSuccess;
}

/**
 * Interrupts build.
 */
public void interruptBuild()  { _javaFileBuilder._interrupt = true; }

/**
 * Removes all build files from project.
 */
public void cleanProject()
{
    // If separate build directory, just delete it
    if(getBuildDir()!=getSourceDir() && getBuildDir()!=getSite().getRootDir())
        try { if(getBuildDir().getExists()) getBuildDir().delete(); }
        catch(Exception e) { throw new RuntimeException(e); }
    
    // Otherwise, remove all class files from build directory
    else removeBuildFiles(getBuildDir());
}

/**
 * Returns the file builder for given file.
 */
public ProjectFileBuilder getFileBuilder(WebFile aFile)
{
    // If file not in source path or already in build path, just return
    String path = aFile.getPath(), sp = getSourceDir().getPath(), bp = getBuildDir().getPath();
    boolean inSrcPath = sp.equals("/") || path.startsWith(sp + "/");
    boolean inBuildPath = path.startsWith(bp + "/") || path.equals(bp);
    if(!inSrcPath || inBuildPath)
        return null;

    // Return JavaFileBuilder, DefaultFileBuilder or null
    if(_javaFileBuilder.isBuildFile(aFile)) return _javaFileBuilder;
    if(_defaultFileBuilder.isBuildFile(aFile)) return _defaultFileBuilder;
    return null;
}

/**
 * Adds a build file.
 */
public void addBuildFilesAll()  { addBuildFile(getSourceDir()); }

/**
 * Adds a build file.
 */
public void addBuildFile(WebFile aFile)
{
    // If file doesn't exist, just return
    if(!aFile.getExists()) return;
    if(aFile.getName().startsWith(".")) return;
    
    // Handle directory
    if(aFile.isDir()) {
        if(aFile==getBuildDir()) return; // If build directory, just return (assuming build dir is in source dir)
        for(WebFile file : aFile.getFiles()) addBuildFile(file);
        return;
    }

    // Get FileBuilder for file and add
    ProjectFileBuilder fileBuilder = getFileBuilder(aFile); if(fileBuilder==null) return;
    if(!fileBuilder.getNeedsBuild(aFile))
        return;
    fileBuilder.addBuildFile(aFile);
}

/**
 * Removes a build file.
 */
protected void removeBuildFile(WebFile aFile)
{
    ProjectFileBuilder fileBuilder = getFileBuilder(aFile);
    if(fileBuilder!=null) fileBuilder.removeBuildFile(aFile);
}

/**
 * Removes all build files from given directory.
 */
private void removeBuildFiles(WebFile aDir)
{
    // Iterate over files and remove class files
    for(int i=aDir.getFileCount()-1; i>=0; i--) { WebFile file = aDir.getFile(i);
        if(file.getType().equals("class"))
            try { file.delete(); }
            catch(Exception e) { throw new RuntimeException(e); }
        else if(file.isDir()) removeBuildFiles(file);
    }
}

/**
 * The breakpoint list property.
 */
public BuildIssues getBuildIssues()  { return _bissues!=null? _bissues : (_bissues=new BuildIssues(this)); }

/**
 * Called when file added.
 */
public void fileAdded(WebFile aFile)
{
    if(aFile.isDir()) readSettings(); 
    addBuildFile(aFile);
}

/**
 * Called when file removed.
 */
public void fileRemoved(WebFile aFile)
{
    removeBuildFile(aFile); // Remove build files
    getRootProject().getBuildIssues().remove(aFile); // Remove BuildIssues for file
}

/**
 * Called when file saved.
 */
public void fileSaved(WebFile aFile)
{
    if(aFile.isDir() || aFile==getClassPath().getFile()) readSettings();
    if(!aFile.isDir()) addBuildFile(aFile);
}

/**
 * Deletes the project.
 */
public void deleteProject(TaskMonitor aTM) throws Exception
{
    aTM.startTasks(1);
    aTM.beginTask("Deleting files", -1);
    clearClassLoader();
    getSite().getSandbox().deleteSite();
    getSite().deleteSite();
    aTM.endTask();
}

/**
 * Standard toString implementation.
 */
public String toString()  { return "Project: " + getSite(); }

/**
 * Returns the project for a given site.
 */
public static Project get(WebFile aFile)  { return get(aFile.getSite()); }

/**
 * Returns the project for a given site.
 */
public static synchronized Project get(WebSite aSite)  { return get(aSite, false); }

/**
 * Returns the project for a given site.
 */
public static synchronized Project get(WebSite aSite, boolean doCreate)
{
    Project proj = (Project)aSite.getProp(Project.class.getName());
    if(proj==null && doCreate) proj = new Project(aSite);
    return proj;
}

}