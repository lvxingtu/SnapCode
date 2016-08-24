/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snap.project;
import java.io.File;
import java.util.List;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.transport.*;
import snap.project.GitDir.*;
import snap.util.*;
import snap.web.*;

/**
 * A VersionControl implementation for Git.
 */
public class VersionControlGit extends VersionControl {

    // The GitDir
    GitDir               _gdir;
    
    // The WebFile for actual .git dir
    WebFile              _gdf;
    
/**
 * Creates a VersionControlGit for a given project.
 */
public VersionControlGit(WebSite aSite)  { super(aSite); }

/**
 * Returns whether existing VCS artifacts are detected for project.
 */
@Override
public boolean getExists()  { return getGitDirFile().getExists(); }

/**
 * Returns the git dir.
 */
public GitDir getGitDir()  { return _gdir!=null? _gdir : (_gdir=GitDir.get(getGitDirFile())); }

/**
 * Returns the git dir.
 */
public WebFile getGitDirFile()
{
    if(_gdf==null) { _gdf = getSite().getFile(".git"); if(_gdf==null) _gdf = getSite().createFile(".git", true); }
    return _gdf;
}

/**
 * Override to return the GitDirSite.
 */
@Override
public WebSite getCloneSite()  { return getGitDir().getIndexSite(); }

/**
 * Override to return the GitDirSite.
 */
@Override
public WebSite getRepoSite()
{
    GitBranch remote = getGitDir().getHead().getBranch().getRemoteBranch();
    GitCommit commit = remote!=null? remote.getCommit() : null;
    return commit!=null? commit.getSite() : null;
}

/**
 * Load all remote files into project directory.
 */
public void checkout(TaskMonitor aTM) throws Exception
{
    // Get SiteDir, CloneDir and RemoteURL
    File sdir = getSite().getRootDir().getStandardFile(), tdir = new File(sdir, "git-temp");
    String uri = getRemoteURL().getString(); if(uri.startsWith("git:")) uri = uri.replace("git:", "https:") + ".git";
    
    // Create CloneCommand and configure
    CloneCommand clone = Git.cloneRepository();
    clone.setURI(uri).setDirectory(tdir).setCredentialsProvider(getCD());
    
    // Wrap TaskMonitor in ProgressMonitor
    if(aTM!=null) clone.setProgressMonitor(getProgressMonitor(aTM));
    
    // Run clone and move files to site directory
    try {
        clone.call().close();
        for(File file : tdir.listFiles())
            FileUtils.move(file, new File(sdir, file.getName()));
    }
    
    // Delete temp directory (should be empty)
    finally { FileUtils.deleteDeep(tdir); }
}

/**
 * Delete VCS support files from project directory.
 */
public void disconnect(TaskMonitor aTM) throws Exception  { getGitDir().deleteDir(); }

/**
 * Returns whether (local) file should be ignored. 
 */
protected boolean isIgnoreFile(WebFile aFile)
{
    if(super.isIgnoreFile(aFile)) return true;
    if(aFile.getName().equals(".git")) return true;
    String gitIgnores[] = getGitIgnoreStrings();
    for(String gi : gitIgnores)
        if(matches(aFile, gi))
            return true;
    return false;
}

/**
 * Returns whether a file matches a given gitignore pattern.
 */
protected boolean matches(WebFile aFile, String aPtrn)
{
    String ptrn = aPtrn; if(ptrn.endsWith("/")) ptrn = ptrn.substring(0, ptrn.length()-1);
    String path = aFile.getPath(), name = aFile.getName();
    return path.equals(ptrn) || name.equals(ptrn);
}

/**
 * Returns the git ignore strings.
 */
protected String[] getGitIgnoreStrings()
{
    WebFile gi = getSite().getFile(".gitignore"); if(gi==null) return new String[0];
    String text = gi.getText();
    return text.split("\\s+");
}

/**
 * Override to do commit.
 */
@Override
public void commitFiles(List <WebFile> theFiles, String aMessage, TaskMonitor aTM) throws Exception
{
    GitDir gdir = getGitDir();
    gdir.commitFiles(theFiles, aMessage);
    gdir.push(aTM);
    
    // Clear file status
    for(WebFile file : theFiles)
        setStatus(file, null);
}

/**
 * Override to do fetch first.
 */
@Override
public void getUpdateFiles(WebFile aFile, List<WebFile> theFiles) throws Exception
{
    // Do git fetch to bring repo up to date
    GitDir gdir = getGitDir();
    gdir.fetch(new TaskMonitor.Text(System.out));

    // Do normal version
    super.getUpdateFiles(aFile, theFiles);
}

/**
 * Override to merge.
 */
@Override
public void updateFiles(List<WebFile> theLocalFiles, TaskMonitor aTM) throws Exception
{
    GitDir gdir = getGitDir();
    gdir.merge();
}

/**
 * Override for bogus implementation that copies clone back to local file.
 */
@Override
public void replaceFile(WebFile aLocalFile) throws Exception
{
    // Get CloneFile
    WebFile cloneFile = getCloneFile(aLocalFile.getPath(), true, aLocalFile.isDir());
    
    // Set new file bytes and save
    if(cloneFile.getExists()) { //_project.removeBuildFile(aLocalFile);
        if(aLocalFile.isFile()) aLocalFile.setBytes(cloneFile.getBytes());
        aLocalFile.save();
        aLocalFile.setLastModifiedTimeDeep(cloneFile.getLastModifiedTime());
        setStatus(aLocalFile, null);
    }
    
    // Otherwise delete LocalFile and CloneFile
    else {
        if(aLocalFile.getExists()) aLocalFile.delete();
        setStatus(aLocalFile, null);
    }
}

/**
 * Returns credentials provider.
 */
private CredentialsProvider getCD()
{
    WebSite rsite = getRemoteSite();
    ClientUtils.setAccess(rsite); if(rsite.getUserName()==null) return null;
    return new UsernamePasswordCredentialsProvider(rsite.getUserName(), rsite.getPassword());
}

/**
 * Returns a ProgressMonitor for given TaskMonitor.
 */
private static ProgressMonitor getProgressMonitor(final TaskMonitor aTM)
{
    return new ProgressMonitor() {
        public void update(int arg0)  { aTM.updateTask(arg0); }
        public void start(int arg0)  { aTM.startTasks(arg0); }
        public boolean isCancelled()  { return aTM.isCancelled(); }
        public void endTask()  { aTM.endTask(); }
        public void beginTask(String arg0, int arg1)  { aTM.beginTask(arg0, arg1); }
    };
}

}