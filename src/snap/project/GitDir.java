package snap.project;
import java.io.File;
import java.util.*;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.dircache.*;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.*;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.*;
import org.eclipse.jgit.treewalk.TreeWalk;
import snap.util.TaskMonitor;
import snap.web.*;
import snap.util.StringUtils;

/**
 * A class to perform git operations for a git directory.
 */
public class GitDir {

    // The git dir
    WebFile                 _gdir;

    // The repository
    Repository              _repo;
    
    // A map of branches
    Map <String,GitBranch>  _branches = new HashMap();
    
    // The index file site
    GitIndex                _index;

    // The index file site
    GitIndexSite            _indexSite;

/**
 * Creates a new Git dir.
 */
public GitDir(WebFile aGitDir)  { _gdir = aGitDir; }

/**
 * Returns the git dir file.
 */
public WebFile getDir()  { return _gdir; }

/**
 * Returns the git.
 */
protected Git getGit()  { return new Git(_repo); }

/**
 * Returns the repo.
 */
protected Repository getRepo()
{
    if(_repo==null) {
        File gdir = getDir().getStandardFile(); if(!gdir.exists()) return null;
        try { _repo = new FileRepositoryBuilder().setGitDir(gdir).readEnvironment().build(); }
        catch(Exception e) { throw new RuntimeException(e); }
    }
    return _repo;
}

/**
 * Returns a git ref for given name.
 */
public GitRef getRef(String aName)
{
    Ref ref = null; try { ref = getRepo().getRef(aName); } catch(Exception e) { throw new RuntimeException(e); }
    return ref!=null? new GitRef(ref) : null;
}

/**
 * Returns the head branch.
 */
public GitRef getHead()  { return getRef(Constants.HEAD); }

/**
 * Returns the named branch.
 */
public GitBranch getBranch(String aName)
{
    Ref ref = null; try { ref = getRepo().getRef(aName); if(ref==null) return null; }
    catch(Exception e) { throw new RuntimeException(e); }
    String name = ref.getTarget().getName();
    GitBranch b = _branches.get(name);
    if(b==null) _branches.put(name, b=new GitBranch(name));
    return b;
}

/**
 * Returns the index.
 */
public GitIndex getIndex()  { return _index!=null? _index : (_index=new GitIndex()); }

/**
 * Returns the index site.
 */
public GitIndexSite getIndexSite()  { return _indexSite!=null? _indexSite : (_indexSite=new GitIndexSite()); }

/**
 * Returns the default remote.
 */
public GitRemote getRemote()  { return getRemote("origin"); }

/**
 * Returns the remote for given name.
 */
public GitRemote getRemote(String aName)  { return new GitRemote(aName); }

/**
 * Override to close repository and delete directory.
 */
public void deleteDir() throws Exception
{
    if(_repo!=null) _repo.close(); _repo = null;
    _gdir.delete();
    _gdir.setProp(GitDir.class.getName(), null);
}

/**
 * Commits a file.
 */
public void commitFiles(List <WebFile> theFiles, String aMessage) throws Exception
{
    // Get repository and git
    Repository repo = getRepo();
    Git git = new Git(repo);
    
    // Add files
    AddCommand add = null;
    for(WebFile file : theFiles)
        if(file.isFile() && file.getExists()) { if(add==null) add = git.add();
            add.addFilepattern(file.getPath().substring(1)); }
    if(add!=null) add.call();
    
    // Remove files
    RmCommand rm = null;
    for(WebFile file : theFiles)
        if(file.isFile() && !file.getExists()) { if(rm==null) rm = git.rm();
           rm.addFilepattern(file.getPath().substring(1)); }
    if(rm!=null) rm.call();
    
    // Commit files
    CommitCommand commit = git.commit();
    commit.setMessage(aMessage);
    commit.setAuthor(new PersonIdent(repo));
    commit.setCommitter(new PersonIdent(repo));
    RevCommit rc = commit.call();
    System.out.println("Commited: " + rc);
    
    // Reset index
    getIndexSite().resetFiles(); _index = null;
}

/**
 * Pushes current committed files.
 */
public void push(TaskMonitor aTM) throws Exception
{
    // Get repository and git
    Git git = getGit();
    
    // Get push
    PushCommand push = git.push();
    push.setProgressMonitor(getProgressMonitor(aTM));
    if(getCD()!=null) push.setCredentialsProvider(getCD());
    for(PushResult pr : push.call())
        System.out.println("Pushed: " + pr);
}

/**
 * Fetches updates to repo.
 */
public void fetch(TaskMonitor aTM) throws Exception
{
    // Do fetch
    Git git = getGit();
    FetchCommand fetch = git.fetch();
    if(getCD()!=null) fetch.setCredentialsProvider(getCD());
    if(aTM!=null) fetch.setProgressMonitor(getProgressMonitor(aTM));
    fetch.call();
    
    // Refresh files
    //getRootDir().refresh();
}

/**
 * Merges updates to working dir and commits.
 */
public void merge() throws Exception
{
    Git git = getGit();
    MergeCommand merge = git.merge();
    ObjectId remoteOriginMaster = getRepo().resolve("refs/remotes/origin/master");
    merge.include(remoteOriginMaster);
    MergeResult result = merge.call();
    System.out.println("Merge Result: " + result.getMergeStatus());
    
    // Reset index
    getIndexSite().resetFiles(); _index = null;
}

/**
 * Returns the RevObject for given commit and path.
 */
private RevObject getRevObject(ObjectId anId)
{
    RevWalk rwalk = new RevWalk(getRepo());
    try { return rwalk.parseAny(anId); }
    catch(Exception e) { throw new RuntimeException(e); }
}

/**
 * Returns credentials provider.
 */
private CredentialsProvider getCD()
{
    //WebSite rsite = getRemoteSite();
    //ClientUtils.setAccess(this); if(rsite.getUserName()==null) return null;
    //return new UsernamePasswordCredentialsProvider(rsite.getUserName(), rsite.getPassword());
    return new UsernamePasswordCredentialsProvider("reportmill", "rmgithub1");
}

/**
 * A class to represent a reference.
 */
public class GitRef {
    
    // The Ref
    Ref        _ref;
    
    // Creates a ref
    GitRef(Ref aRef)  { _ref = aRef; }
    
    /** Returns the name. */
    public String getName()  { return _ref.getName(); }
    
    /** Returns the branch for ref. */
    public GitBranch getBranch()  { return GitDir.this.getBranch(getName()); }
    
    /** Standard toString implementation. */
    public String toString() { return _ref.toString(); }
}

/**
 * A class to represent a remote.
 */
public class GitRemote {
    
    // The name
    String        _name;
    
    /** Creates a remote for name. */
    GitRemote(String aName)  { _name = aName; }
    
    /** Returns the name. */
    public String getName()  { return _name; }
    
    /** Returns a branch for name. */
    public GitBranch getBranch(String aName)  { return GitDir.this.getBranch(getName() + '/' + aName); }
}

/**
 * A class to represent a branch.
 */
public class GitBranch {
    
    // The name of the branch
    String         _name;
    
    /** Creates new branch for name. */
    public GitBranch(String aName)  { _name = aName; }
    
    /** Returns the full branch name. */
    public String getName()  { return _name; }
    
    /** Returns the simple branch name. */
    public String getSimpleName()  { return StringUtils.getPathFileName(_name); }
    
    /** Returns the plain branch name (no refs/heads or ref/remotes prefix). */
    public String getPlainName()  { return getName().replace("refs/heads/", "").replace("refs/remotes/", ""); }
    
    /** Returns the Commit. */
    public GitCommit getCommit()
    {
        ObjectId id = null; try { id = getRepo().resolve(_name); }
        catch(Exception e)  { throw new RuntimeException(e); }
        RevCommit rc = (RevCommit)getRevObject(id);
        return new GitCommit(rc);
    }
    
    /** Returns the list of all commits for branch. */
    public GitCommit[] getCommits()
    {
        List <GitCommit> list = new ArrayList(); for(GitCommit c=getCommit(); c!=null; c = c.getParent()) list.add(c);
        return list.toArray(new GitCommit[list.size()]);
    }
    
    /** Returns the remote tracking branch. */
    public GitBranch getRemoteBranch()
    {
        if(getName().contains("/remotes/")) return null;
        return getRemote().getBranch(getSimpleName());
    }
}

/**
 * A class to represent a file.
 */
public class GitFile <T extends RevObject> {
    
    // The RevObject
    T         _rev;
    
    // The path
    String    _path;
    
    /** Returns the path. */
    public String getPath()  { return _path; }
    
    /** Returns the resource name. */
    public String getName()  { return StringUtils.getPathFileName(getPath()); }
    
    /** Returns whether file is directory. */
    public boolean isDir()  { return false; }
    
    /** Returns whether file is file (blob). */
    public boolean isFile()  { return false; }
    
    /** Returns the list of child files. */
    public GitFile[] getFiles()  { return null; }
    
    /** Returns the bytes. */
    public byte[] getBytes()  { return null; }
    
    /** Standard toString implementation. */
    public String toString()  { return getClass().getSimpleName() + ": " + _path + ", " + _rev; }
}

/**
 * A class to represent a commit file.
 */
public class GitCommit extends GitFile <RevCommit> {
    
    // The Parent commit
    GitCommit    _par;
    
    // The Tree
    GitTree      _tree;
    
    // The TreeSite
    GitFileSite  _site;
    
    /** Creates a new GitCommit. */
    GitCommit(RevCommit anRC)  { _rev = anRC; }
    
    /** Returns the commit time. */
    public long getCommitTime()  { return _rev.getCommitTime()*1000L; }
    
    /** Returns the parent commit. */
    public GitCommit getParent()  { return _par!=null? _par : (_par=getParentImpl()); }
    
    /** Returns the parent commit. */
    private GitCommit getParentImpl()
    {
        RevCommit r = _rev.getParentCount()>0? _rev.getParent(0) : null;
        if(r!=null) r = (RevCommit)getRevObject(r); // They return a rev commit, but it isn't loaded!
        return r!=null? new GitCommit(r) : null;
    }
    
    /** Returns the tree. */
    public GitTree getTree()  { return _tree!=null? _tree : (_tree=new GitTree(_rev.getTree(), "/")); }
    
    /** Returns the site. */
    public GitFileSite getSite()  { return _site!=null? _site : (_site=new GitFileSite(this)); }
}

/**
 * A class to represent a tree file.
 */
public class GitTree extends GitFile <RevTree> {
    
    // The child files
    GitFile      _files[];
    
    /** Creates a new GitTree. */
    GitTree(RevTree aRT, String aPath)  { _rev = aRT; _path = aPath; }
    
    /** Returns whether file is directory. */
    @Override
    public boolean isDir()  { return true; }
    
    /** Returns the list of child files. */
    @Override
    public GitFile[] getFiles()
    {
        if(_files==null) try { _files = getFilesImpl(); } catch(Exception e) { throw new RuntimeException(e); }
        return _files;
    }
        
    /** Returns the list of child files. */
    GitFile[] getFilesImpl() throws Exception
    {
        TreeWalk twalk = new TreeWalk(getRepo()); twalk.addTree(_rev);
        List <GitFile> files = new ArrayList();
        while(twalk.next()) {
            ObjectId id = twalk.getObjectId(0);
            RevObject rid = getRevObject(id);
            String path = _path + (_path.length()>1? "/" : "") + twalk.getNameString();
            GitFile child = rid instanceof RevTree? new GitTree((RevTree)rid, path) : new GitBlob((RevBlob)rid, path);
            files.add(child);
        }
        return files.toArray(new GitFile[files.size()]);
    }
    
    /** Returns a file for a given path. */
    public GitFile getFile(String aPath)
    {
        if(aPath.equals("/")) return this;
        String paths[] = aPath.split("/"); GitFile file = this;
        for(int i=0; i<paths.length && file!=null; i++) { String name = paths[i]; if(name.length()==0) continue;
            GitFile files[] = file.getFiles(); file = null;
            for(GitFile f : files)
                if(name.equals(f.getName())) {
                    file = f; break; }
        }
        return file;
    }
}

/**
 * A class to represent a blob.
 */
public class GitBlob extends GitFile <RevBlob> {
    
    /** Creates a new GitBlob. */
    GitBlob(RevBlob aRB, String aPath)  { _rev = aRB; _path = aPath; }
    
    /** Returns whether file is file (blob). */
    public boolean isFile()  { return true; }
    
    /** Returns the bytes. */
    public byte[] getBytes()
    {
        try { return getRepo().newObjectReader().open(_rev).getBytes(); }
        catch(Exception e) { throw new RuntimeException(e); }
    }
}

/**
 * A class to represent a GitIndex.
 */
public class GitIndex {
    
    // The repository index
    DirCache         _index;
    
    /** Returns the index. */
    public DirCache getIndex()
    {
        if(_index==null) try { _index = getRepo().readDirCache(); }
        catch(Exception e) { throw new RuntimeException(e); }
        return _index;
    }

    /** Returns an index entry for given path. */
    public Entry getEntry(String aPath)
    {
        if(getRepo()==null) return null;
        
        // Handle root special
        if(aPath.equals("/")) return new Entry(null, aPath);
    
        // Get repository index and entry for path
        DirCache index = getIndex(); String path = aPath.substring(1);
        DirCacheEntry entry = index.getEntry(aPath.substring(1));
        boolean isDir = entry==null && index.getEntriesWithin(path).length>0;
        if(entry==null && !isDir) return null;
        
        // Create file for path and index entry
        return new Entry(entry, aPath);
    }
    
    /** Returns child entries. */
    protected Entry[] getEntries(Entry anEntry)
    {
            // Get repository index and entry for path
        DirCache index = getIndex(); String path = anEntry.getPath().substring(1);
        DirCacheEntry entries[] = index.getEntriesWithin(path);

        // Interate over entries
        List <Entry> list = new ArrayList(); String lastPath = "";
        for(DirCacheEntry entry : entries) {
            String epath = entry.getPathString();
            int ind = epath.indexOf('/', path.length()+1); if(ind>0) epath = epath.substring(0, ind);
            if(epath.equals(lastPath)) continue; lastPath = epath;
            Entry child = new Entry(ind<0? entry : null, '/' + epath);
            list.add(child);
        }
        
        // Return list
        return list.toArray(new Entry[list.size()]);
    }
    
    /**
     * A class to represent a GitIndex entry.
     */
    public class Entry {
    
        // The DirCacheEntry
        DirCacheEntry _entry; String _path;
        
        /** Creates a new GitIndex Entry for given DirCacheEntry. */
        public Entry(DirCacheEntry aDCE, String aPath)  { _entry = aDCE; _path = aPath; }
        
        /** Returns the path. */
        public String getPath()  { return _path; }
        
        /** Returns the name. */
        public String getName()  { return StringUtils.getPathFileName(_path); }
        
        /** Returns whether entry is directory. */
        public boolean isDir()  { return _entry==null; }
        
        /** Returns the file length. */
        public int getLength()  { return _entry!=null? _entry.getLength() : 0; }
        
        /** Returns the last modified time. */
        public long getLastModified()  { return _entry!=null? _entry.getLastModified() : 0; }
        
        /** Returns the bytes for entry. */
        public byte[] getBytes()
        {
            try { return getRepo().newObjectReader().open(_entry.getObjectId()).getBytes(); }
            catch(Exception e) { throw new RuntimeException(e); }
        }
        
        /** Returns a list of entries. */
        public Entry[] getEntries()  { return GitIndex.this.getEntries(this); }
        
        /** Standard toString implementation. */
        public String toString()  { return "Entry: " + _path + ", " + _entry; }
    }
}

/**
 * A WebSite implementation for a GitCommit.
 */
public class GitFileSite extends WebSite {
    
    // The GitCommit
    GitCommit _cmt;
    
    /** Creates a new GitFileSite for GitCommit. */
    public GitFileSite(GitCommit aGC)  { _cmt = aGC; setURL(createURL()); }
    
    /** Creates URL for site. */
    WebURL createURL()
    {
        String url = getDir().getURL().getString() + "!/" + _cmt._rev.getId().getName();
        return WebURL.getURL(url);
    }
    
    /** Returns the tree for this site. */
    public GitTree getTree()  { return _cmt.getTree(); }
    
    /** Get file from directory. */
    protected FileHeader getFileHeader(String aPath) throws Exception
    {
        // Get Head branch Commit Tree and look for file
        GitFile gfile = getTree().getFile(aPath); if(gfile==null) return null;
        
        // Create file for path and commit time
        FileHeader file = new FileHeader(aPath, gfile.isDir());
        file.setLastModifiedTime(_cmt.getCommitTime());
        return file;
    }
    
    /** Get file from directory. */
    protected List <FileHeader> getFileHeaders(String aPath) throws Exception
    {
        // Get root tree and look for file
        GitFile gfile = getTree().getFile(aPath); if(gfile==null || !gfile.isDir()) return null;
    
        // Walk RevTree and get files for children
        List <FileHeader> files = new ArrayList();
        for(GitFile gf : gfile.getFiles()) {
            FileHeader child = getFileHeader(gf.getPath());
            files.add(child);
        }
        
        // Return files
        return files;
    }
    
    /** Return file bytes. */
    protected byte[] getFileBytes(String aPath) throws Exception
    {
        GitFile gfile = getTree().getFile(aPath); if(gfile==null) return null;
        return gfile.getBytes();
    }
}

/**
 * A WebSite implementation for GitDirIndex.
 */
protected class GitIndexSite extends WebSite {

    /** Creates a GitIndexSite. */
    public GitIndexSite()  { setURL(WebURL.getURL(getDir().getURL().getString() + ".index")); }
    
    /** Get file from directory. */
    protected FileHeader getFileHeader(String aPath) throws Exception
    {
        GitIndex.Entry entry = getIndex().getEntry(aPath); if(entry==null) return null;
        FileHeader file = new FileHeader(aPath, entry.isDir());
        file.setLastModifiedTime(entry.getLastModified()); file.setSize(entry.getLength());
        return file;
    }
    
    /** Get file headers for directory path files. */
    protected List <FileHeader> getFileHeaders(String aPath) throws Exception
    {
        // Get GitIndex.Entry for path and iterate over Entry.Entries (children)
        GitIndex.Entry entry = getIndex().getEntry(aPath); if(entry==null || !entry.isDir()) return null;
        List <FileHeader> files = new ArrayList(); String lastPath = "";
        for(GitIndex.Entry child : entry.getEntries()) {
            FileHeader file = getFileHeader(child.getPath());
            files.add(file);
        }
        
        // Return files
        return files;
    }
    
    /** Return file bytes. */
    protected byte[] getFileBytes(String aPath) throws Exception
    {
        GitIndex.Entry entry = getIndex().getEntry(aPath); if(entry==null || entry.isDir()) return null;
        return entry.getBytes();
    }
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

/**
 * Returns a GitDir for a git directory file.
 */
public synchronized static GitDir get(WebFile aFile)
{
    GitDir gdir = (GitDir)aFile.getProp(GitDir.class.getName());
    if(gdir==null) aFile.setProp(GitDir.class.getName(), gdir=new GitDir(aFile));
    return gdir;
}

}