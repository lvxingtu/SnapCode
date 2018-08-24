package snap.app;
import snap.project.Project;
import snap.util.*;
import snap.web.*;

/**
 * A class to help AppLauncher with JS launch.
 */
public class AppLauncherJS extends AppLauncher {

/**
 * Creates a new AppLauncherJS.
 */
public AppLauncherJS(AppLauncher anAL)
{
    _config = anAL._config; _file = anAL._file; _url = anAL._url; _proj = anAL._proj;
}

/**
 * Returns whether this project includes CheerpJ support.
 */
public static boolean isJS(Project aProj)
{
    // If class path includes JSAPI project, return true
    String cpath = FilePathUtils.getJoinedPath(aProj.getProjectSet().getLibPaths());
    return aProj.getName().equals("JSAPI") || cpath.contains("JSAPI");
}

/**
 * Runs the provided file as CheerpJ app in browser.
 */
void runJS(AppPane anAppPane)
{
    buildJSFiles();
    compileDone();
}

/**
 * Builds the JS files.
 */
public void buildJSFiles()
{
    WebFile binDir = _proj.getBuildDir();
    WebFile srcDir = _proj.getSourceDir();
    for(WebFile file : srcDir.getFiles()) {
        if(file.getType().equals("java")) {
            snap.javakit.JavaData jdata = snap.javakit.JavaData.get(file);
            String str = new snap.javascript.JSWriter().getString(jdata.getJFile());
            WebFile jsFile = binDir.getSite().createFile(binDir.getDirPath() + file.getSimpleName() + ".js", false);
            jsFile.setBytes(str.getBytes());
            jsFile.save();
        }
    }
}

/**
 * Called when compile finishes.
 */
public void compileDone()
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
    sb.append("<meta charset=\"utf-8\">\n");
    sb.append("</head>\n");
    sb.append("<body>\n");
    sb.append("</body>\n");
    
    //sb.append("<script src=\"https://cjrtnc.leaningtech.com/latest/loader.js\"></script>\n");
    //sb.append("<script>\n"); sb.append("cheerpjInit();\n"); sb.append("</script>\n");
    
    WebFile srcDir = _proj.getSourceDir();
    for(WebFile file : srcDir.getFiles()) {
        if(file.getType().equals("java"))
            sb.append("<script src=\"" + file.getSimpleName() + ".js" + "\"></script>\n");
    }
    
    sb.append("<script>\n");
    sb.append(classNameSimple).append(".main(null);\n");
    sb.append("</script>\n");
    
    sb.append("</html>");
    hfile.setText(sb.toString());
    hfile.save();
    return hfile;
}

}