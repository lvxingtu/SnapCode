/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snap.typescript;
import java.util.*;
import snap.javakit.JavaData;
import snap.project.*;
import snap.util.*;
import snap.web.WebFile;

/**
 * A FileBuilder to build TypeScript files.
 */
public class TSFileBuilder implements ProjectFileBuilder {

    // The project we work for
    Project                  _proj;

    // A list of files to be compiled
    Set <WebFile>            _buildFiles = Collections.synchronizedSet(new HashSet());
    
    // Whether to interrupt current build
    boolean                  _interrupt;
    
    // The TypeWriter to convert files to type script
    TSWriter                 _twriter = new TSWriter();
    
/**
 * Creates a new TSFileBuilder for given Project.
 */
public TSFileBuilder(Project aProject)  { _proj = aProject; }

/**
 * Returns whether file is build file.
 */
public boolean isBuildFile(WebFile aFile)  { return aFile.getType().equals("java"); }

/**
 * Returns whether given file needs to be built.
 */
public boolean getNeedsBuild(WebFile aFile)
{
    // See if Java file has out of date TS file 
    WebFile tsfile = getTSFile(aFile);
    boolean needsBuild = !tsfile.getExists() || tsfile.getLastModifiedTime()<aFile.getLastModifiedTime();
    
    // Return NeedsBuild
    return needsBuild;
}

/**
 * Adds a compile file.
 */
public void addBuildFile(WebFile aFile)  { _buildFiles.add(aFile); }

/**
 * Remove a build file.
 */
public void removeBuildFile(WebFile aFile)
{
    // Remove from build files
    _buildFiles.remove(aFile);
    
    // Get dependent files and add to BuildFiles
    JavaData jdata = JavaData.get(aFile);
    Set <WebFile> dependents = jdata.getDependents();
    for(WebFile dependant : dependents)
        if(dependant.getExists())
            addBuildFile(dependant);
    
    // Remove JavaFile Dependencies
    //jdata.removeDependencies();
}

/**
 * Compiles files.
 */
public boolean buildFiles(TaskMonitor aTaskMonitor)
{
    if(_buildFiles.size()==0) return true;
    List <WebFile> files = new ArrayList(_buildFiles); _buildFiles.clear();
    
    // Add known build files to FileManager, to force use of JavaFile instead of ClassFile, regardless of ModTime
    _interrupt = false;
    
    // Iterate over build files and compile
    boolean compileSuccess = true; //long time = System.currentTimeMillis();
    for(int i=0; i<files.size(); i++) { WebFile file = files.get(i);
    
        // If interrupted, add remaining build files and return
        if(_interrupt) {
            for(int j=i, jMax=files.size(); j<jMax; j++) addBuildFile(files.get(j)); return false; }
        
        // Convert file
        JavaData jdata = JavaData.get(file);
        String tscript = _twriter.getString(jdata.getJFile());
        WebFile tsfile = getTSFile(file);
        //if(SnapUtils.equals(tscript, tsfile.getText()))
        //    continue;
        tsfile.setText(tscript);
        tsfile.save();
    }
    
    // Finalize ActivityText and return
    return compileSuccess;
}

/**
 * Returns the TypeScript file for given file.
 */
public WebFile getTSFile(WebFile aFile)
{
    String aPath = aFile.getPath().replace(".java", ".ts");
    
    // Look for file in build dir
    String path = aPath, spath = _proj.getSourceDir().getDirPath(), bpath = _proj.getBuildDir().getPath();
    if(spath.length()>1 && path.startsWith(spath)) path = path.substring(spath.length() - 1);
    if(bpath.length()>1 && !path.startsWith(bpath)) path = bpath + "/ts" + path;
    WebFile file = _proj.getBuildDir().getSite().getFile(path);
    
    // If file still not found, maybe create and return
    if(file==null) file = _proj.getBuildDir().getSite().createFile(path, false);
    return file;
}

}