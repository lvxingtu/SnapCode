package snap.app;
import java.io.File;
import java.util.*;
import snap.debug.*;
import snap.project.*;
import snap.util.*;
import snap.web.*;

/**
 * A class to launch Snap apps.
 */
public class AppLauncher {

    // The run config
    RunConfig         _config;

    // The file to launch
    WebFile            _file;
    
    // The url to launch
    WebURL            _url;
    
    // The Project
    Project           _proj;
    
    // The last executed file
    static WebFile    _lastRunFile;
    
/**
 * Returns the WebFile.
 */
public WebFile getFile()  { return _file; }

/**
 * Returns the WebURL.
 */
public WebURL getURL()  { return _file.getURL(); }

/**
 * Returns the URL String.
 */
public String getURLString()  { return getURL().getString(); }

/**
 * Returns the app args.
 */
public String getAppArgs()  { return _config!=null? _config.getAppArgs() : null; }

/**
 * Runs the provided file for given run mode.
 */
public void runFile(AppPane anAppPane, RunConfig aConfig, WebFile aFile, boolean isDebug, boolean inBrowser)
{
    // Have AppPane save files
    anAppPane.saveFiles();
    
    // Get file
    WebFile file = aFile, bfile;
    
    // Try to replace file with project file
    _proj = Project.get(file); if(_proj==null) { System.err.println("AppLauncher: not project file: " + file); return; }
    if(file.getType().equals("java")) bfile = _proj.getClassFile(file);
    else bfile = _proj.getBuildFile(file.getPath(), false, file.isDir());
    if(bfile!=null) file = bfile;
    
    // Set URL
    _file = file;
    _url = _file.getURL();
    _config = aConfig;
    
    // Set last run file
    _lastRunFile = aFile;
    
    // If Cheerp (links against SnapCJ), generate cheerp files and open in browser
    if(inBrowser) {
    
        // If CheerpJ support, generate cheerp files and open in browser
        if(AppLauncherCJ.isCheerp(_proj)) {
            new AppLauncherCJ(this).runCheerp(anAppPane); return; }
            
        // If CheerpJ support, generate cheerp files and open in browser
        if(AppLauncherTVM.isTeaVM(_proj)) {
            new AppLauncherTVM(this).runTea(anAppPane); return; }
        anAppPane.beep(); return;
    }
        
    // If CheerpJ support and file invokes it, generate cheerp files and open in browser
    if(AppLauncherCJ.isCheerp(_proj, aFile)) {
        new AppLauncherCJ(this).runCheerp(anAppPane); return; }
        
    // If TeaVM support and file invokes it, generate cheerp files and open in browser
    if(AppLauncherTVM.isTeaVM(_proj, aFile)) {
        new AppLauncherTVM(this).runTea(anAppPane); return; }
        
    // If HTML
    if(AppLauncherTVM.isTeaHTML(_proj, aFile)) {
        new AppLauncherTVM(this).runTeaHTML(aFile); return; }
    
    // Run/debug file
    if(isDebug) debugApp(anAppPane);
    else runApp(anAppPane);
}

/**
 * Runs the provided file as straight app.
 */
void runApp(AppPane anAppPane)
{
    // Get run command as string array
    List <String> commands = getCommand();
    String command[] = commands.toArray(new String[commands.size()]);
    
    // Print run command to console
    System.err.println(ListUtils.joinStrings(ListUtils.newList((Object[])command), " "));
    
    // Create RunApp and exec
    RunApp proc = new RunApp(getURL(), command);
    anAppPane.getProcPane().execProc(proc);
}

/**
 * Runs the provided file as straight app.
 */
void debugApp(AppPane anAppPane)
{
    // Get run command as string array (minus actual run)
    List <String> commands = getDebugCommand();
    String command[] = commands.toArray(new String[commands.size()]);
    
    // Print run command to console
    System.err.println("debug " + ListUtils.joinStrings(ListUtils.newList((Object[])command), " "));
    
    // Create DebugApp and add project breakpoints
    DebugApp proc = new DebugApp(getURL(), command);
    for(Breakpoint bp : _proj.getBreakpoints())
        proc.addBreakpoint(bp);
        
    // Add app to process pane and exec
    anAppPane.getProcPane().execProc(proc);
}

/**
 * Returns an array of args.
 */
protected List <String> getCommand()
{
    // Get basic run command and add to list
    List <String> commands = new ArrayList();
    String java = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
    commands.add(java);
    
    // Get Class path and add to list
    String cpaths[] = _proj.getProjectSet().getClassPaths(), cpathsNtv[] = FilePathUtils.getNativePaths(cpaths);
    String cpath = FilePathUtils.getJoinedPath(cpathsNtv);
    commands.add("-cp"); commands.add(cpath);

    // Add class name
    commands.add(_proj.getClassName(getFile()));
    
    // Add App Args
    if(getAppArgs()!=null && getAppArgs().length()>0)
        commands.add(getAppArgs());
    
    // Return commands
    return commands;
}

/**
 * Returns an array of args.
 */
protected List <String> getDebugCommand()  { List <String> cmd = getCommand(); cmd.remove(0); return cmd; }

/**
 * Returns the last run file.
 */
public static WebFile getLastRunFile()  { return _lastRunFile; }

}