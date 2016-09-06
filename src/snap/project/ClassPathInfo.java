package snap.project;
import java.util.*;
import snap.util.*;
import snap.web.*;

/**
 * A class to return class file info for Project class paths.
 */
public class ClassPathInfo implements PropChangeListener {

    // The Project
    Project             _proj;

    // The shared list of class path sites
    List <WebSite>      _sites = new ArrayList();
    
    // The list of all package files and class files
    List <WebFile>      _apkgs, _acls;
    
/**
 * Creates a new new ClassPathInfo for project class paths.
 */
public ClassPathInfo(Project aProj)
{
    // Set project
    _proj = aProj;
    
    // Add system jar sites
    WebSite javart = WebURL.getURL(List.class).getSite(); _sites.add(javart);
    WebSite jfxrt = WebURL.getURL(javafx.scene.Node.class).getSite(); _sites.add(jfxrt);
    
    // Add project class path sites
    for(String jar : aProj.getProjectSet().getClassPaths())
        _sites.add(WebURL.getURL(jar).getAsSite());
}

/**
 * Returns the project.
 */
public Project getProject()  { return _proj; }

/**
 * Returns the class path sites.
 */
public List <WebSite> getSites()  { return _sites; }

/**
 * Returns class names for prefix.
 */
public List <String> getPackageClassNames(String aPkgName, String aPrefix)
{
    WebFile pkgDir = getPackageDir(aPkgName); if(pkgDir==null) return Collections.emptyList();
    List <WebFile> cfiles = getClassFiles(pkgDir.getFiles(), aPrefix);
    return getClassNames(cfiles);
}

/**
 * Returns packages for prefix.
 */
public List <String> getPackageChildrenNames(String aPkgName, String aPrefix)
{
    WebFile pkgDir = getPackageDir(aPkgName); if(pkgDir==null) return Collections.emptyList();
    List <WebFile> pfiles = getChildPackages(pkgDir.getFiles(), aPrefix);
    return getPackageNames(pfiles);
}

/**
 * Returns all packages with prefix.
 */
public List <String> getAllPackageNames(String aPrefix)
{
    List <WebFile> pfiles = getChildPackages(getAllPackages(), aPrefix);
    return getPackageNames(pfiles);
}

/**
 * Returns all classes with prefix.
 */
public List <String> getAllClassNames(String aPrefix)
{
    List <WebFile> cfiles = getClassFiles(getAllClasses(), aPrefix);
    return getClassNames(cfiles);
}

/**
 * Returns class names for entries list.
 */
private List <String> getClassNames(List <WebFile> theFiles)
{
    List <String> names = new ArrayList(theFiles.size());
    for(WebFile file : theFiles) { String path = file.getPath(), path2 = path.substring(1,path.length()-6);
        names.add(path2.replace('/', '.')); }
    return names;
}

/**
 * Returns packages for entries list.
 */
private List <String> getPackageNames(List <WebFile> theFiles)
{
    List names = new ArrayList(theFiles.size());
    for(WebFile pfile : theFiles) names.add(pfile.getPath().substring(1).replace('/', '.'));
    return names;
}

/**
 * Returns a package dir for a package name.
 */
public WebFile getPackageDir(String aName)
{
    String path = "/" + aName.replace('.', '/');
    List <WebSite> sites = getSites();
    for(WebSite site : sites) { WebFile file = site.getFile(path);
        if(file!=null) return file; }
    return null;
}

/**
 * Returns a list of class files for a package dir and a prefix.
 */
public List <WebFile> getClassFiles(List <WebFile> theFiles, String aPrefix)
{
    List cfiles = new ArrayList();
    for(WebFile file : theFiles) {
        String name = file.getName(); int di = name.lastIndexOf('$'); if(di>0) name = name.substring(di+1);
        if(StringUtils.startsWithIC(name, aPrefix) && name.endsWith(".class"))
            cfiles.add(file);
    }
    return cfiles;
}

/**
 * Returns a list of class files for a package dir and a prefix.
 */
public List <WebFile> getChildPackages(List <WebFile> theFiles, String aPrefix)
{
    List pfiles = new ArrayList();
    for(WebFile file : theFiles)
        if(file.isDir() && StringUtils.startsWithIC(file.getName(), aPrefix) && file.getName().indexOf('.')<0)
            pfiles.add(file);
    return pfiles;
}

/**
 * Returns the list of all packages.
 */
public List <WebFile> getAllPackages()  { if(_apkgs==null) createAll(); return _apkgs; }

/**
 * Returns the list of all classes.
 */
public List <WebFile> getAllClasses()  { if(_acls==null) createAll(); return _acls; }

protected void createAll()
{
    _acls = new ArrayList(); _apkgs = new ArrayList();
    for(WebSite site : getSites()) getAll(site.getRootDir(), _acls, _apkgs);
}

private void getAll(WebFile aDir, List <WebFile> theClasses, List <WebFile> thePkgs)
{
    for(WebFile file : aDir.getFiles()) {
        if(file.isDir()) {
            if(file.getName().indexOf('.')>0) continue;
            if(thePkgs!=null) thePkgs.add(file);
            getAll(file, theClasses, null); // Send null because we only want top level packages
        }
        else {
            String path = file.getPath();
            if(!path.endsWith(".class")) continue;
            if(!isInterestingPath(path)) continue;
            theClasses.add(file);
        }
    }
}

/**
 * Adds an entry (override to ignore).
 */
private static boolean isInterestingPath(String aPath)
{
    if(aPath.startsWith("/sun")) return false;
    if(aPath.startsWith("/apple")) return false;
    if(aPath.startsWith("/com/sun")) return false;
    if(aPath.startsWith("/com/apple")) return false;
    if(aPath.startsWith("/com/oracle")) return false;
    if(aPath.startsWith("/javax/swing/plaf")) return false;
    if(aPath.startsWith("/org/omg")) return false;
    int dollar = aPath.endsWith(".class")? aPath.lastIndexOf('$') : -1;
    if(dollar>0 && Character.isDigit(aPath.charAt(dollar+1))) return false;
    return true;
}

/**
 * Watches Project.ClassPath for JarPaths change to reset ClassPathInfo.
 */
public void propertyChange(PropChange anEvent)
{
    if(anEvent.getPropertyName()==ClassPath.JarPaths_Prop) {
        _proj.getSite().setProp("ClassPathInfo", null);
        _proj.getClassPath().removePropChangeListener(this);
    }
}

/**
 * Standard toString implementation.
 */
public String toString()  { return getClass().getSimpleName() + ": " + getSites(); }

/**
 * Returns the ClassPathInfo for a JNode.
 */
public static ClassPathInfo get(WebSite aSite)
{
    ClassPathInfo cpinfo = (ClassPathInfo)aSite.getProp("ClassPathInfo");
    if(cpinfo==null) {
        Project proj = Project.get(aSite);
        aSite.setProp("ClassPathInfo", cpinfo = new ClassPathInfo(proj));
        proj.getClassPath().addPropChangeListener(cpinfo);
    }
    return cpinfo;
}

}