/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snap.project;
import java.util.*;
import snap.util.*;
import snap.web.WebFile;

/**
 * An interface for classes that can "build" changed files.
 */
public interface ProjectFileBuilder {

    /**
     * Returns whether file is build file.
     */
    public boolean isBuildFile(WebFile aFile);

    /**
     * Returns whether given file needs a build.
     */
    public boolean getNeedsBuild(WebFile aFile);

    /**
     * Add a build file.
     */
    public void addBuildFile(WebFile aFile);
    
    /**
     * Remove a build file.
     */
    public void removeBuildFile(WebFile aFile);
    
    /**
     * Build files.
     */
    public boolean buildFiles(TaskMonitor aTM);
    
/**
 * A FileBuilder to build miscellaneous files.
 */
public static class DefaultBuilder implements ProjectFileBuilder {

    // The site we work for
    Project                _proj;

    // A list of files to be built
    List <WebFile>        _buildFiles = new ArrayList();
    
    /** Creates a new DefaultFileBuilder for given Project. */
    public DefaultBuilder(Project aProject)  { _proj = aProject; }

    /** Returns whether file is build file. */
    public boolean isBuildFile(WebFile aFile)
    {
        if(aFile.getPath().equals("/Project.settings")) return false;
        return true;
    }

    /** Returns whether given file needs to be built. */
    public boolean getNeedsBuild(WebFile aFile)
    {
        WebFile bfile = _proj.getBuildFile(aFile.getPath(), false, false);
        return bfile==null || !bfile.getExists() || bfile.getLastModifiedTime()<aFile.getLastModifiedTime();
    }

    /** Adds a compile file. */
    public void addBuildFile(WebFile aFile)  { ListUtils.addUniqueId(_buildFiles, aFile); }

    /** Remove a build file. */
    public void removeBuildFile(WebFile aFile)
    {
        WebFile bfile = _proj.getBuildFile(aFile.getPath(), false, false); if(bfile==null) return;
        try { if(bfile.getExists()) bfile.delete(); }
        catch(Exception e) { throw new RuntimeException(e); }
    }
    
    /** Compiles files. */
    public boolean buildFiles(TaskMonitor aTM)
    {
        // If no build files, just return
        if(_buildFiles.size()==0) return true;
        
        // Get build files array (and clear list)
        WebFile buildFiles[] = _buildFiles.toArray(new WebFile[_buildFiles.size()]); _buildFiles.clear();
        
        // Copy file to build directory
        boolean success = true;
        for(WebFile file : buildFiles) {
            WebFile buildFile = _proj.getBuildFile(file.getPath(), true, file.isDir());
            if(buildFile.isFile()) buildFile.setBytes(file.getBytes());
            try { buildFile.save(); }
            catch(Exception e) { success = false; break; }
        }
        
        // Return true
        return success;
    }
}

}