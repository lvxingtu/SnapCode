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

    // The file to make an applet for
    WebURL            _url;
    
    // The Project
    Project           _proj;
    
    // The last executed file
    static WebFile    _lastRunFile;
    
/**
 * Returns the WebURL.
 */
public WebURL getURL()  { return _url; }

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
public void runFile(AppPane anAppPane, RunConfig aConfig, WebFile aFile, boolean isDebug)
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
    _url = file.getURL();
    _config = aConfig;
    
    // Set last run file
    _lastRunFile = aFile;
    
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
    
    // Create RunApp
    RunApp proc = new RunApp(getURL(), command);
    anAppPane.getProcPane().addProc(proc);
    anAppPane.getProcPane().setSelApp(proc);
    anAppPane.setSupportTrayIndex(1);
    proc.exec();
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
    anAppPane.getProcPane().addProc(proc);
    anAppPane.getProcPane().setSelApp(proc);
    anAppPane.setSupportTrayIndex(1);
    proc.exec();
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
    List <String> cpaths = new ArrayList();
    Collections.addAll(cpaths, _proj.getClassPathPaths());
    String cpath = ListUtils.joinStrings(cpaths, File.pathSeparator);
    commands.add("-cp"); commands.add(cpath);

    // If using Snap Runtime, add main class
    if(_proj.getUseSnapRuntime() || !getURLString().endsWith(".class")) {
        commands.add("snap.swing.SnapApp");
        commands.add("file"); commands.add(getURLString()); // Add test file path
    }
    
    // Otherwise, add class name
    else commands.add(_proj.getClassName(getURL().getFile()));
    
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