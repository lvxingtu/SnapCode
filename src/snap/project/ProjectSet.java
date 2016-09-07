package snap.project;
import java.util.*;
import snap.util.ListUtils;
import snap.util.TaskMonitor;
import snap.web.*;

/**
 * Manages a project and the set of projects it depends on.
 */
public class ProjectSet {

    // The master project
    Project             _proj;
    
    // The list of projects this project depends on
    Project             _projects[];
    
    // The array of class paths and library paths
    String              _cpaths[], _lpaths[];

/**
 * Creates a new ProjectSet for given Project.
 */
public ProjectSet(Project aProj)  { _proj = aProj; }

/**
 * Returns the project.
 */
public Project getProject()  { return _proj; }

/**
 * Returns the list of projects this project depends on.
 */
public Project[] getProjects()
{
    // If already set, just return
    if(_projects!=null) return _projects;
    
    // Create list of projects from ClassPath.ProjectPaths
    List <Project> projs = new ArrayList();
    for(String path : _proj.getClassPath().getProjectPaths()) {
        WebSite parSite = _proj.getSite().getURL().getSite(); // Get parent site
        WebURL projURL = parSite.getURL(path);
        WebSite projSite = projURL.getAsSite();
        Project proj = Project.get(projSite, true); proj._parent = _proj;
        ListUtils.addAllUnique(projs, proj.getProjectSet().getProjects());
        ListUtils.addUnique(projs, proj);
    }
    
    // Return list
    return _projects = projs.toArray(new Project[projs.size()]);
}

/**
 * Adds a dependent project.
 */
public void addProject(String aPath)
{
    _proj.getClassPath().addSrcPath(aPath);
    _projects = null; _cpaths = _lpaths = null;
}

/**
 * Removes a dependent project.
 */
public void removeProject(String aPath)
{
    _proj.getClassPath().removeSrcPath(aPath);
    _projects = null; _cpaths = _lpaths = null;
}

/**
 * Returns a file for given path.
 */
public WebFile getFile(String aPath)
{
    WebFile file = _proj.getFile(aPath); if(file!=null) return file;
    for(Project p : getProjects()) {
        file = p.getFile(aPath); if(file!=null) return file; }
    return null;
}

/**
 * Returns the source file for given path.
 */
public WebFile getSourceFile(String aPath, boolean doCreate, boolean isDir)
{
    // Look for file in source dir
    WebFile file = _proj.getSourceFile(aPath, false, isDir);
    
    // Look for file in dependent child projects
    if(file==null)
        for(Project proj : getProjects())
            if((file=proj.getSourceFile(aPath, false, isDir))!=null) break;
    
    // If file still not found, maybe create and return
    if(file==null && doCreate) file = _proj.getSourceFile(aPath, true, isDir);
    return file;
}

/**
 * Returns the build file for given path.
 */
public WebFile getBuildFile(String aPath, boolean doCreate, boolean isDir)
{
    // Look for file in root project
    WebFile file = _proj.getBuildFile(aPath, false, isDir);
    
    // Look for file in dependent child projects
    if(file==null)
        for(Project proj : getProjects())
            if((file=proj.getBuildFile(aPath, false, isDir))!=null) break;
    
    // If file still not found, maybe create and return
    if(file==null && doCreate) file = _proj.getBuildFile(aPath, true, isDir);;
    return file;
}

/**
 * Returns the paths needed to compile/run project.
 */
public String[] getClassPaths()
{
    if(_cpaths!=null) return _cpaths;
    String cpaths[] = _proj.getClassPaths();
    Project projs[] = getProjects(); if(projs.length==0) return _cpaths = cpaths;
    List <String> paths = new ArrayList(); Collections.addAll(paths, cpaths);
    for(Project p : getProjects()) ListUtils.addAllUnique(paths, p.getClassPaths());
    return _cpaths = paths.toArray(new String[paths.size()]);
}

/**
 * Returns the paths needed to compile/run project, except build directory.
 */
public String[] getLibPaths()
{
    if(_lpaths!=null) return _lpaths;
    String lpaths[] = _proj.getLibPaths();
    Project projs[] = getProjects(); if(projs.length==0) return _lpaths = lpaths;
    List <String> paths = new ArrayList(); Collections.addAll(paths, lpaths);
    for(Project p : projs) ListUtils.addAllUnique(paths, p.getClassPaths());
    return _lpaths = paths.toArray(new String[paths.size()]);
}

/**
 * Adds a build file.
 */
public void addBuildFilesAll()
{
    _proj.addBuildFilesAll();
    for(Project p : getProjects())
        p.addBuildFilesAll();
}

/**
 * Builds the project.
 */
public boolean buildProjects(TaskMonitor aTM)
{
    for(Project p : getProjects())
        if(!p.buildProject(aTM))
            return false;
    return _proj.buildProject(aTM);
}
    
/**
 * Returns a Java file for class name.
 */
public WebFile getJavaFile(String aClassName)
{
    WebFile file = _proj.getJavaFile(aClassName); if(file!=null) return file;
    for(Project p : getProjects()) { file = p.getJavaFile(aClassName); if(file!=null) return file; }
    return null;
}

}