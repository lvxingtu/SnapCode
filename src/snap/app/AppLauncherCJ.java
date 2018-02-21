package snap.app;
import java.io.File;
import java.util.*;
import snap.debug.RunApp;
import snap.javakit.*;
import snap.project.Project;
import snap.util.*;
import snap.view.ViewUtils;
import snap.viewx.DialogBox;
import snap.web.*;

/**
 * A class to help AppLauncher with CheerpJ launch.
 */
public class AppLauncherCJ extends AppLauncher {

/**
 * Creates a new AppLauncherCJ.
 */
public AppLauncherCJ(AppLauncher anAL)  { _config = anAL._config; _url = anAL._url; _proj = anAL._proj; }

/**
 * Returns whether this project includes CheerpJ support.
 */
public static boolean isCheerp(Project aProj)
{
    // If class path doesn't include SnapCJ jar, return false
    String cpath = FilePathUtils.getJoinedPath(aProj.getProjectSet().getLibPaths());
    return cpath.contains("SnapCJ");
}

/**
 * Returns whether this project includes CheerpJ and given file invokes it.
 */
public static boolean isCheerp(Project aProj, WebFile aFile)
{
    // If class path doesn't include SnapCJ jar, return false
    if(!isCheerp(aProj)) return false;
    
    // If main class contains snapcj.CJ.set() reference, return true
    Set <JavaDecl> decls = JavaData.get(aFile).getRefs(); for(JavaDecl decl : decls) {
        if(decl.getName().startsWith("snapcj.CJ")) return true; }
    return false;
}

/**
 * Runs the provided file as CheerpJ app in browser.
 */
void runCheerp(AppPane anAppPane)
{
    // If no python, just bail
    if(getPythonPath()==null) return;
    
    // Build Jar file
    try { JarBuilder.build(_proj); }
    catch(Exception e) { throw new RuntimeException(e); }
    
    // Ensure runtime libs are present
    checkRuntime();
    
    // Get build path, cheerp path
    String buildPath = _proj.getClassPath().getBuildPathAbsolute();
    String cheerpPath =  buildPath + "/cheerp";
    
    // Get run command as string array
    List <String> commands = getCheerpCommand();
    String command[] = commands.toArray(new String[commands.size()]);
    
    // Create RunApp
    RunApp proc = new RunApp(getURL(), command);
    proc.setWorkingDirectory(_proj.getClassPath().getBuildPathAbsolute());
    anAppPane.getProcPane().execProc(proc);
    proc.addListener(new RunApp.AppAdapter() {
        public void appExited(RunApp ra) { cheerpCompileDone(); }});
}

/**
 * Ensure runtime libs are present: SnapKit.jar/js, CJDom.jar/js, SnapCJ.jar.js.
 */
void checkRuntime()
{
    // Get lib paths
    String cpath = FilePathUtils.getJoinedPath(_proj.getProjectSet().getLibPaths());
    
    // Check for SnapKit
    if(cpath.contains("SnapKit")) {
        checkRuntimeFile("SnapKit.jar", "http://reportmill.com/cj/SnapKit/SnapKit.jar");
        checkRuntimeFile("SnapKit.jar.js", "http://reportmill.com/cj/SnapKit/SnapKit.jar.js");
    }
    
    // Check for CJDom
    if(cpath.contains("CJDom")) {
        checkRuntimeFile("CJDom.jar", "http://reportmill.com/cj/CJDom/CJDom.jar");
        checkRuntimeFile("CJDom.jar.js", "http://reportmill.com/cj/CJDom/CJDom.jar.js");
    }
    
    // Check for SnapCJ
    if(cpath.contains("SnapCJ")) {
        checkRuntimeFile("SnapCJ.jar", "http://reportmill.com/cj/SnapCJ/SnapCJ.jar");
        checkRuntimeFile("SnapCJ.jar.js", "http://reportmill.com/cj/SnapCJ/SnapCJ.jar.js");
    }
}

/**
 * Ensure runtime libs are present: SnapKit.jar/js, CJDom.jar/js, SnapCJ.jar.js.
 */
void checkRuntimeFile(String aName, String aURL)
{
    // Get path for name and return if file exists
    String path = "/jars/" + aName;
    if(_proj.getBuildFile(path, false, false)!=null) return;
    
    // Get URL for remote file (check for SnapCode/jar_cache/name for bogus JavaOne optimization)
    WebURL url = WebURL.getURL(aURL);
    String localCachePath = FilePathUtils.getChild(SnapCodeUtils.getSnapCodeDirPath(), "jar_cache/" + aName);
    WebURL localCacheURL = WebURL.getURL(localCachePath);
    if(localCacheURL.isFound())
        url = localCacheURL;
    
    // Get bytes for remote file
    byte bytes[] = url.getBytes();
    
    // Create local file, set bytes and save
    WebFile file = _proj.getBuildFile(path, true, false);
    file.setBytes(bytes);
    file.save();
    System.out.println("Downloaded runtime file: " + file.getPath());
}

/**
 * Returns an array of args for CheerpJ.
 * prompt> python3 /Temp/tools/cheerpj/cheerpjfy.py --deps SnapKit.jar:CJDom.jar:SnapCJ.jar CodeFun.jar
 */
protected List <String> getCheerpCommand()
{
    // Get basic python command and add to list
    List <String> commands = new ArrayList();
    String python = getPythonPath();
    commands.add(python);
    
    // Get cheerpjfy compiler path and to list
    String cheerpCompilerPath = FilePathUtils.getChild(SnapCodeUtils.getSnapCodeDirPath(), "cheerpj/cheerpjfy.py");
    String cheerpCompilerPathNtv = FilePathUtils.getNativePath(cheerpCompilerPath);
    commands.add(cheerpCompilerPathNtv);
    
    // Get Class path and add to list
    String cpath = "jars/SnapKit.jar:jars/CJDom.jar:jars/SnapCJ.jar".replace(':', FilePathUtils.PATH_SEPARATOR_CHAR);
    commands.add("--deps"); commands.add(cpath);

    // Get jar name
    String jarName = _proj.getName() + ".jar";
    
    // If alt is down, pack jar
    if(ViewUtils.isAltDown()) {
        commands.add("--pack-jar");
        commands.add(jarName);
    }
    
    // Add jar name
    commands.add(jarName);
    
    // Return commands
    return commands;
}

/**
 * Called when cheerp compile finishes.
 */
public void cheerpCompileDone()
{
    // Start HttpServer
    SitePane.get(_url.getSite()).getHttpServerPane().startServer();
    
    // Get HTML file and open
    WebFile htmlFile = getHTMLFile();
    WebURL htmlURL = getHTMLURL();
    snap.gfx.GFXEnv.getEnv().openURL(htmlURL);
}

/**
 * Creates and returns an HTML file for given name.
 */
public WebURL getHTMLURL()
{
    String className = _url.getPathNameSimple();
    String htmlPath = "http://127.0.0.1:8080/" + className + ".html";
    return WebURL.getURL(htmlPath);
}

/**
 * Creates and returns an HTML file for given name.
 */
public WebFile getHTMLFile()
{
    String cpath = FilePathUtils.getJoinedPath(_proj.getProjectSet().getLibPaths());
    if(cpath.contains("SnapCJ"))
        return getHTMLFileSnapKit();
    return getHTMLFileSwing();
}

/**
 * Creates and returns an HTML file for given name.
 */
public WebFile getHTMLFileSnapKit()
{
    String classNameSimple = _url.getPathNameSimple();
    String htmlPath = _proj.getClassPath().getBuildPathAbsolute() + '/' + classNameSimple + ".html";
    WebURL html = WebURL.getURL(htmlPath);
    WebFile hfile = html.getFile(); //if(hfile!=null) return hfile;
    hfile = html.createFile(false);
    
    // Get Main class and jar path
    String className = _proj.getClassName(getURL().getFile());
    String jarName = _proj.getName() + ".jar";
    String jarPath = "jars/SnapKit.jar:jars/CJDom.jar:jars/SnapCJ.jar:" + jarName;

    StringBuffer sb = new StringBuffer();
    sb.append("<!DOCTYPE html>\n<html>\n<head>\n<title>" + classNameSimple + " CheerpJ</title>\n");
    sb.append("<meta http-equiv=\"Content-Type\" content=\"text/html;charset=utf-8\">\n");
    sb.append("<script src=\"https://cheerpjdeploy.leaningtech.com/loader.js\" crossorigin=\"anonymous\"></script>\n");
    sb.append("</head>\n");
    sb.append("<body>\n");
    sb.append("</body>\n");
    sb.append("<script src=\"http://reportmill.com/cj/scripts/Loader.js\"></script>\n");
    sb.append("<script>\n");
    sb.append("snapRunJar(\"" + className + "\",\"" + jarPath + "\");\n");
    sb.append("</script>\n");
    sb.append("</html>");
    hfile.setText(sb.toString());
    hfile.save();
    return hfile;
}

/**
 * Creates and returns an HTML file for given name.
 */
public WebFile getHTMLFileSwing()
{
    String classNameSimple = _url.getPathNameSimple();
    String htmlPath = _proj.getClassPath().getBuildPathAbsolute() + '/' + classNameSimple + ".html";
    WebURL html = WebURL.getURL(htmlPath);
    WebFile hfile = html.getFile(); //if(hfile!=null) return hfile;
    hfile = html.createFile(false);
    
    // Get Main class and jar path
    String className = _proj.getClassName(getURL().getFile());
    String jarName = _proj.getName() + ".jar";
    String jarPath = "jars/SnapKit.jar:" + jarName;

    StringBuffer sb = new StringBuffer();
    sb.append("<!DOCTYPE html>\n<html>\n<head>\n<title>" + classNameSimple + " CheerpJ</title>\n");
    sb.append("<meta http-equiv=\"Content-Type\" content=\"text/html;charset=utf-8\">\n");
    sb.append("<script src=\"https://cheerpjdeploy.leaningtech.com/loader.js\" crossorigin=\"anonymous\"></script>\n");
    sb.append("</head>\n");
    sb.append("<body>\n");
    sb.append("</body>\n");
    sb.append("<script src=\"http://reportmill.com/cj/scripts/Loader.js\"></script>\n");
    sb.append("<script>\n");
    sb.append("snapRunJarSwing(\"" + className + "\",\"" + jarPath + "\",1000,1000);\n");
    sb.append("</script>\n");
    sb.append("</html>");
    hfile.setText(sb.toString());
    hfile.save();
    return hfile;
}

/**
 * Returns the path to python.
 */
static String getPythonPath()
{
    if(_pyPath!=null) return _pyPath;
    _pyPath = findExecutableOnPath("python3");
    String msg = "Can't find the python3 command - please download and try again. Search path: "+System.getenv("PATH");
    if(_pyPath==null) DialogBox.showConfirmDialog(null,"Can't find Python", msg);
    return _pyPath;
}
static String _pyPath;

public static String findExecutableOnPath(String aName)
{
    String paths[] = System.getenv("PATH").split(java.io.File.pathSeparator);
    List <String> plist = new ArrayList(); Collections.addAll(plist,paths);
    String name = aName; if(SnapUtils.isWindows) name += ".exe"; else plist.add("/usr/local/bin");

    for (String dirname : plist) {
        File file = new File(dirname, name);
        if(file.isFile() && file.canExecute())
            return file.getAbsolutePath();
    }
    
    return null;
}

}