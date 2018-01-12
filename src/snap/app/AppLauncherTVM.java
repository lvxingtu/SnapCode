package snap.app;
import java.io.File;
import java.util.*;
import snap.debug.RunApp;
import snap.javakit.*;
import snap.project.Project;
import snap.util.*;
import snap.view.DialogBox;
import snap.web.*;

/**
 * A class to help AppLauncher with TVM launch.
 */
public class AppLauncherTVM extends AppLauncher {

    // AppLauncher
    AppLauncher     _appLauncher;
    
/**
 * Creates a new AppLauncherTVM.
 */
public AppLauncherTVM(AppLauncher anAL)
{
    _config = anAL._config; _file = anAL._file; _url = anAL._url; _proj = anAL._proj;
}

/**
 * Returns whether this project includes TeaVM support.
 */
public static boolean isTeaVM(Project aProj)
{
    String cpath = FilePathUtils.getJoinedPath(aProj.getProjectSet().getLibPaths());
    return cpath.contains("teavm-");
}

/**
 * Returns whether this project includes TeaVM support and given file invokes it.
 */
public static boolean isTeaVM(Project aProj, WebFile aFile)
{
    // If class path doesn't include TeaVM jar, return false
    if(!isTeaVM(aProj)) return false;
    
    // If main class contains TVViewEnv, return true
    Set <JavaDecl> decls = JavaData.get(aFile).getRefs(); for(JavaDecl decl : decls) {
        if(decl.getName().startsWith("snaptea.TV"))
            return true; }
    return false;
}

/**
 * Runs the provided file as straight app.
 */
void runTea(AppPane anAppPane)
{
    // Update Tea files
    updateTeaFiles();
    
    // Get run command as string array
    List <String> commands = getTeaCommand();
    String command[] = commands.toArray(new String[commands.size()]);
    
    // Print run command to console
    System.err.println(ListUtils.joinStrings(ListUtils.newList((Object[])command), " "));
    
    // Create RunApp
    RunApp proc = new RunApp(getURL(), command);
    anAppPane.getProcPane().execProc(proc);
    proc.addListener(new RunApp.AppAdapter() {
        public void appExited(RunApp ra) { teaCompileDone(); }});
}

/**
 * Returns an array of args.
 */
protected List <String> getTeaCommand()
{
    // Get basic run command and add to list
    List <String> commands = new ArrayList();
    String java = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
    commands.add(java);
    
    // Get Class path and add to list
    String cpaths[] = _proj.getProjectSet().getClassPaths(); //cpaths = ArrayUtils.add(cpaths, "/Temp/teavm/*");
    String cpathsNtv[] = FilePathUtils.getNativePaths(cpaths);
    String cpath = FilePathUtils.getJoinedPath(cpathsNtv); cpath = cpath.replace("teavm-jso.jar", "*");
    commands.add("-cp"); commands.add(cpath);
    
    // Add runner and class name
    commands.add("org.teavm.cli.TeaVMRunner");
    commands.add(_proj.getClassName(getURL().getFile()));
    
    // Add output dir
    String bpath = _proj.getClassPath().getBuildPathAbsolute() + "tea";
    String bpathNtv = FilePathUtils.getNativePath(bpath);
    commands.add("-d"); commands.add(bpathNtv);
    
    // Add Preserve Class
    commands.add("-preserve-class"); commands.add("snaptea.TV");
    
    // Add other options
    //commands.add("-S"); commands.add("-D");
    commands.add("-G"); commands.add("-g");
    
    // Add App Args
    if(getAppArgs()!=null && getAppArgs().length()>0)
        commands.add(getAppArgs());
    
    // Return commands
    return commands;
}

/**
 * Called when tea compile is done.
 */
public void teaCompileDone()
{
    // Start HttpServer
    SitePane.get(_url.getSite()).getHttpServerPane().startServer();
    
    // Get HTML file and open
    WebFile htmlFile = getHTMLIndexFile();
    WebURL htmlURL = getHTMLURL();
    snap.gfx.GFXEnv.getEnv().openURL(htmlURL);

    //WebFile htmlFile = getHTMLIndexFile();
    //snap.gfx.GFXEnv.getEnv().openFile(htmlFile);
}

/**
 * Creates and returns an HTML file for given name.
 */
public WebURL getHTMLURL()
{
    //String className = _url.getPathNameSimple();
    String htmlPath = "http://127.0.0.1:8080/tea/index.html"; // + className + ".html";
    return WebURL.getURL(htmlPath);
}

/**
 * Updates tea vm files.
 */
private void updateTeaFiles()
{
    updateTeaFiles(_proj.getSourceDir());
    for(Project proj : _proj.getProjects()) {
        WebFile file = proj.getSourceDir();
        /*if(proj.getName().equals("SnapKit")) {
            updateTeaFiles(proj.getFile("/src/snap/util/XMLParser.txt"));
            updateTeaFiles(proj.getFile("/src/snap/view"));
            updateTeaFiles(proj.getFile("/src/snap/viewx/JTokens.txt"));
            updateTeaFiles(proj.getFile("/src/snap/viewx/TextPane.snp"));
            updateTeaFiles(proj.getFile("/src/snap/viewx/pkg.images/Edit_Copy.png"));
            updateTeaFiles(proj.getFile("/src/snap/viewx/pkg.images/Edit_Cut.png"));
            updateTeaFiles(proj.getFile("/src/snap/viewx/pkg.images/Edit_Delete.png"));
            updateTeaFiles(proj.getFile("/src/snap/viewx/pkg.images/Edit_Paste.png"));
            updateTeaFiles(proj.getFile("/src/snap/viewx/pkg.images/Edit_Redo.png"));
            updateTeaFiles(proj.getFile("/src/snap/viewx/pkg.images/Edit_Undo.png"));
            updateTeaFiles(proj.getFile("/src/snap/viewx/pkg.images/File_New.png"));
            updateTeaFiles(proj.getFile("/src/snap/viewx/pkg.images/File_Open.png"));
            updateTeaFiles(proj.getFile("/src/snap/viewx/pkg.images/File_Print.png"));
            updateTeaFiles(proj.getFile("/src/snap/viewx/pkg.images/File_Save.png"));
            updateTeaFiles(proj.getFile("/src/snap/viewx/pkg.images/Font_Increase.png"));
            updateTeaFiles(proj.getFile("/src/snap/viewx/pkg.images/Font_Decrease.png"));
        } else */
        updateTeaFiles(file);
    }
    
    // Write tea file
    WebURL teaIndexURL = WebURL.getURL(_proj.getClassPath().getBuildPathAbsolute() + "/tea/index.txt");
    WebFile teaIndex = teaIndexURL.createFile(false);
    String text = ListUtils.joinStrings(_teaPaths, "\n");
    if(!text.equals(teaIndex.getText())) {
        teaIndex.setText(text);
        teaIndex.save();
    }
}

/**
 * Updates tea vm files.
 */
private void updateTeaFiles(WebFile aFile)
{
    // If directory, just recurse
    if(aFile.isDir()) {
        String name = aFile.getName(); if(name.equals("bin") || name.equals("tea")) return;
        for(WebFile file : aFile.getFiles()) updateTeaFiles(file);
    }
    
    // Otherwise get tea build file and see if it needs to be updated
    else if(isResourceFile(aFile)) {
        String path = aFile.getPath(); if(path.startsWith("/src/")) path = path.substring(4);
        WebURL url = WebURL.getURL(_proj.getClassPath().getBuildPathAbsolute() + "/tea" + path);
        WebFile tfile = url.getFile();
        if(tfile==null || aFile.getLastModTime()>tfile.getLastModTime()) {
            //System.out.println("Updating Tea Resource File: " + url.getPath());
            if(tfile==null) tfile = url.createFile(false);
            tfile.setBytes(aFile.getBytes());
            tfile.save();
        }
        _teaPaths.add(path);
    }
}

List <String> _teaPaths = new ArrayList();

/**
 * Returns whether given file is a resource file.
 */
public boolean isResourceFile(WebFile aFile)  { return !aFile.getType().equals("java"); }

/**
 * Returns whether this is TeaVM launch.
 */
public static boolean isTeaHTML(Project aProj, WebFile aFile)
{
    // If class path doesn't include TeaVM jar, return false
    String cpath = FilePathUtils.getJoinedPath(aProj.getProjectSet().getLibPaths());
    if(!cpath.contains("teavm-")) return false;
    
    // If main class contains TVViewEnv, return true
    return aFile.getType().equals("html") || aFile.getType().equals("snp");
}

/**
 * Runs the provided file as straight app.
 */
void runTeaHTML(WebFile aFile)
{
    // If snp file, offer to create HTML
    if(aFile.getType().equals("snp")) {
        String htmlFilename = aFile.getSimpleName() + ".html";
        WebFile htmlFile = aFile.getParent().getFile(htmlFilename);
        if(htmlFile==null)
            if(!DialogBox.showConfirmDialog(null, "Create HTML file", "Create HTML file?")) return;
        aFile = createHTMLFile(aFile);
    }
    
    WebURL turl = WebURL.getURL(_proj.getClassPath().getBuildPathAbsolute() + "/tea");
    WebFile tdir = turl.getFile();
    if(tdir==null) { }
    
    // Update Tea files
    updateTeaFiles();
    
    WebURL html = WebURL.getURL(_proj.getClassPath().getBuildPathAbsolute() + "/tea/" + aFile.getName());
    WebFile htmlFile = html.getFile();
    snap.gfx.GFXEnv.getEnv().openFile(htmlFile);
}

/**
 * Creates and returns an HTML file for given name.
 */
public WebFile getHTMLIndexFile()
{
    WebURL html = WebURL.getURL(_proj.getClassPath().getBuildPathAbsolute() + "/tea/index.html");
    WebFile hfile = html.getFile(); if(hfile!=null) return hfile;
    hfile = html.createFile(false);

    StringBuffer sb = new StringBuffer();
    sb.append("<!DOCTYPE html>\n<html>\n<head>\n<title>SnapTea</title>\n");
    sb.append("<meta http-equiv=\"Content-Type\" content=\"text/html;charset=utf-8\">\n");
    sb.append("<script type=\"text/javascript\" charset=\"utf-8\" src=\"runtime.js\"></script>\n");
    sb.append("<script type=\"text/javascript\" charset=\"utf-8\" src=\"classes.js\"></script>\n");
    sb.append("</head>\n<body onload=\"main()\">\n"); sb.append("</body>\n</html>");
    hfile.setText(sb.toString());
    hfile.save();
    return hfile;
}

/**
 * Creates and returns an HTML file for given name.
 */
public WebFile createHTMLFile(WebFile aFile)
{
    String hpath = aFile.getParent().getDirPath() + aFile.getSimpleName() + ".html";
    WebFile hfile = aFile.getSite().createFile(hpath, false);

    StringBuffer sb = new StringBuffer();
    sb.append("<!DOCTYPE html>\n<html>\n<head>\n<title>SnapTea</title>\n");
    sb.append("<meta http-equiv=\"Content-Type\" content=\"text/html;charset=utf-8\">\n");
    sb.append("<script type=\"text/javascript\" charset=\"utf-8\" src=\"runtime.js\"></script>\n");
    sb.append("<script type=\"text/javascript\" charset=\"utf-8\" src=\"classes.js\"></script>\n");
    sb.append("</head>\n<body onload=\"load()\">\n");
    sb.append("<script type=\"text/javascript\">\n");
    sb.append("    function load() { main(\"" + aFile.getName() + "\"); }\n");
    sb.append("</script>\n"); sb.append("</body>\n</html>");
    hfile.setText(sb.toString());
    hfile.save();
    return hfile;
}

}