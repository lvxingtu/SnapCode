package snap.app;
import java.util.*;
import snap.debug.RunApp;
import snap.project.Project;
import snap.util.*;
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
 * Returns whether this is Cheerp launch.
 */
public static boolean isCheerp(Project aProj, WebFile aFile)
{
    // If class path doesn't include SnapCJ jar, return false
    String cpath = FilePathUtils.getJoinedPath(aProj.getProjectSet().getLibPaths());
    //if(!cpath.contains("SnapCJ")) return false;
    if(cpath.contains("SnapCJ")) return true;
    
    // If main class contains snapcj.CJ.set() reference, return true
    //Set <JavaDecl> decls = JavaData.get(aFile).getRefs(); for(JavaDecl decl : decls) {
    //    if(decl.getName().startsWith("snapcj.CJ")) return true; }
    return false;
}

/**
 * Runs the provided file as CheerpJ app in browser.
 */
void runCheerp(AppPane anAppPane)
{
    // Build Jar file
    try { JarBuilder.build(_proj); }
    catch(Exception e) { throw new RuntimeException(e); }
    
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
 * Returns an array of args for CheerpJ.
 * prompt> python3 /Temp/tools/cheerpj/cheerpjfy.py --deps SnapKit.jar:CJDom.jar:SnapCJ.jar CodeFun.jar
 */
protected List <String> getCheerpCommand()
{
    // Get basic python command and add to list
    List <String> commands = new ArrayList();
    String python = "python3";
    commands.add(python);
    
    // Get cheerpjfy compiler path and to list
    String cheerpCompilerPath = FilePathUtils.getChild(SnapCodeUtils.getSnapCodeDirPath(), "cheerpj/cheerpjfy.py");
    String cheerpCompilerPathNtv = FilePathUtils.getNativePath(cheerpCompilerPath);
    commands.add(cheerpCompilerPathNtv);
    
    // Get Class path and add to list
    String cpath = "SnapKit.jar:CJDom.jar:SnapCJ.jar".replace(':', FilePathUtils.PATH_SEPARATOR_CHAR);
    commands.add("--deps"); commands.add(cpath);
    
    // Add Jar name
    String jarPath = _proj.getName() + ".jar";
    commands.add(jarPath);
    
    // Return commands
    return commands;
}

/**
 * Called when cheerp compile finishes.
 */
public void cheerpCompileDone()
{
    WebFile htmlFile = getCheerpHTMLFile();
    WebURL htmlURL = getCheerpHTMLURL();
    snap.gfx.GFXEnv.getEnv().openURL(htmlURL);
}

/**
 * Creates and returns an HTML file for given name.
 */
public WebURL getCheerpHTMLURL()
{
    String className = _url.getPathNameSimple();
    String htmlPath = "http://127.0.0.1:8080/" + className + ".html";
    return WebURL.getURL(htmlPath);
}

/**
 * Creates and returns an HTML file for given name.
 */
public WebFile getCheerpHTMLFile()
{
    String cpath = FilePathUtils.getJoinedPath(_proj.getProjectSet().getLibPaths());
    if(cpath.contains("SnapCJ"))
        return getCheerpHTMLFileSnapKit();
    return getCheerpHTMLFileSwing();
}

/**
 * Creates and returns an HTML file for given name.
 */
public WebFile getCheerpHTMLFileSnapKit()
{
    String classNameSimple = _url.getPathNameSimple();
    String htmlPath = _proj.getClassPath().getBuildPathAbsolute() + '/' + classNameSimple + ".html";
    WebURL html = WebURL.getURL(htmlPath);
    WebFile hfile = html.getFile(); //if(hfile!=null) return hfile;
    hfile = html.createFile(false);
    
    // Get Main class and jar path
    String className = _proj.getClassName(getURL().getFile());
    String jarName = _proj.getName() + ".jar";
    String jarPath = "SnapKit.jar:CJDom.jar:SnapCJ.jar:" + jarName;

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
public WebFile getCheerpHTMLFileSwing()
{
    String classNameSimple = _url.getPathNameSimple();
    String htmlPath = _proj.getClassPath().getBuildPathAbsolute() + '/' + classNameSimple + ".html";
    WebURL html = WebURL.getURL(htmlPath);
    WebFile hfile = html.getFile(); //if(hfile!=null) return hfile;
    hfile = html.createFile(false);
    
    // Get Main class and jar path
    String className = _proj.getClassName(getURL().getFile());
    String jarName = _proj.getName() + ".jar";
    String jarPath = "SnapKit.jar:" + jarName;

    StringBuffer sb = new StringBuffer();
    sb.append("<!DOCTYPE html>\n<html>\n<head>\n<title>" + classNameSimple + " CheerpJ</title>\n");
    sb.append("<meta http-equiv=\"Content-Type\" content=\"text/html;charset=utf-8\">\n");
    sb.append("<script src=\"https://cheerpjdeploy.leaningtech.com/loader.js\" crossorigin=\"anonymous\"></script>\n");
    sb.append("</head>\n");
    sb.append("<body>\n");
    sb.append("</body>\n");
    sb.append("<script src=\"http://reportmill.com/cj/scripts/Loader.js\"></script>\n");
    sb.append("<script>\n");
    sb.append("snapRunJarSwing(\"" + className + "\",\"" + jarPath + "\");\n");
    sb.append("</script>\n");
    sb.append("</html>");
    hfile.setText(sb.toString());
    hfile.save();
    return hfile;
}

}