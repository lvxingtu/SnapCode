package snap.app;
import java.text.SimpleDateFormat;
import java.util.*;
import snap.project.*;
import snap.project.GitDir.*;
import snap.view.*;
import snap.viewx.*;
import snap.web.*;

/**
 * VcsPane subclass to show UI for Git respository.
 */
public class VcsPaneGit extends VcsPane {
    
    // The VersionControlGit
    VersionControlGit  _vcg;
    
    // The GitDir
    GitDir             _gdir;

    // The BrowserView to show Git info
    BrowserView        _gitBrowser;
    
    // The WebBrowser to show current selection
    WebBrowser         _selBrowser;
    
/**
 * Creates a new VcsPaneGit for given SitePane.
 */
public VcsPaneGit(SitePane aSP)
{
    super(aSP);
    _vcg = (VersionControlGit)VersionControl.get(getSite());
    _gdir = _vcg.getGitDir();
}

/**
 * Called when checkout succeeds.
 */
@Override
protected void checkoutSuccess(boolean oldAutoBuildEnabled)
{
    super.checkoutSuccess(oldAutoBuildEnabled);
    _gitBrowser.setItems(getRootItems());
}

/** Override to suppress. */
@Override
public void connectToRemoteSite()  { }

/**
 * Create UI.
 */
protected View createUI()
{
    // Get UI and main pane
    ParentView superUI = (ParentView)super.createUI();
    SpringView pane = (SpringView)superUI.getChild(superUI.getChildCount()-1);
    
    // Add Vertical SplitView
    SplitView splitView = new SplitView(); splitView.setVertical(true); splitView.setAutosizing("-~-,-~~");
    splitView.setBounds(5,5,pane.getWidth()-10, pane.getHeight()-10);
    pane.addChild(splitView);
    
    // Create/add GitBrowser BrowserView
    _gitBrowser = new BrowserView(); _gitBrowser.setName("GitBrowser");
    _gitBrowser.setPrefHeight(300); _gitBrowser.setPrefColCount(3);
    _gitBrowser.setResolver(new GitDirResolver());
    splitView.addItem(_gitBrowser);
    
    // Create/add SelBrowser WebBrowser
    _selBrowser = new WebBrowser(); _selBrowser.setGrowHeight(true);
    splitView.addItem(_selBrowser);
    
    // Return UI
    return superUI;
}

/**
 * Initialize UI.
 */
protected void initUI()
{
    // Initialize GitBrowser with GitDir RootItems
    _gitBrowser.setItems(getRootItems());
    
    // Clear ProgressBar
    getView("ProgressBar").setVisible(false);
}

/**
 * Reset UI controls.
 */
public void resetUI()
{
    // Update RemoteURLText
    setViewValue("RemoteURLText", _vc.getRemoteURLString());
}

/**
 * Respond to UI change.
 */
public void respondUI(ViewEvent anEvent)
{
    // Handle GitBrowser
    if(anEvent.equals("GitBrowser")) {
        
        // Get selected item
        Object item = anEvent.getSelectedItem(); Object items[] = null;
        if(item instanceof GitRef) item = ((GitRef)item).getBranch();
        
        // Handle GitFile
        if(item instanceof GitFile) { GitFile gf = (GitFile)item;
            GitCommit gc = gf.isFile()? getSelectedParent(GitCommit.class) : null;
            if(gc!=null) {
                WebSite site = gc.getSite();
                WebFile file = site.getFile(gf.getPath());
                _selBrowser.getHistory().clearHistory();
                _selBrowser.setFile(file);
            }
        }
        
        // Handle GitIndex.Entry
        else if(item instanceof GitIndex.Entry) { GitIndex.Entry gie = (GitIndex.Entry)item;
            WebSite site = _vcg.getGitDir().getIndexSite();
            WebFile file = site.getFile(gie.getPath());
            _selBrowser.getHistory().clearHistory();
            _selBrowser.setFile(file);
        }
    }
    
    // Handle SyncButton
    else if(anEvent.equals("SyncButton"))
        getUpdateFiles(null);
    
    // Handle Update/Commit/Replace buttons
    else super.respondUI(anEvent);
}

/**
 * Returns the Root items for GitDir.
 */
protected Object[] getRootItems()
{
    List items = new ArrayList(); if(!_vc.getExists()) return new Object[0];
    items.add(_gdir.getHead());
    items.add(_gdir.getIndex());
    return items.toArray();
}

/**
 * Returns the parent for current GitBrowser.SelectedItem (as given class).
 */
public <T> T getSelectedParent(Class <T> aClass)
{
    for(int i=_gitBrowser.getSelColIndex()-1;i>=0;i--) { BrowserCol bcol = _gitBrowser.getCol(i);
        Object item = bcol.getSelectedItem();
        if(aClass.isInstance(item))
            return (T)item; }
    return null;
}

/**
 * The TreeResolver to provide data to File browser.
 */
private class GitDirResolver extends TreeResolver {
    
    /** Returns the parent of given item. */
    public Object getParent(Object anItem)  { return null; }

    /** Whether given object is a parent (has children). */
    public boolean isParent(Object anItem)  { return getChildren(anItem)!=null; }

    /** Returns the children. */
    public Object[] getChildren(Object aPar)
    {
         Object item = aPar, items[] = null;
        if(item instanceof GitRef) item = ((GitRef)item).getBranch();
        if(item instanceof GitBranch) {
            items = ((GitBranch)item).getCommits();
            GitBranch rb = ((GitBranch)item).getRemoteBranch(); if(rb!=null) items = add(items, rb);
        }
        else if(item instanceof GitCommit) items = ((GitCommit)item).getTree().getFiles();
        else if(item instanceof GitFile) items = ((GitFile)item).isDir()? ((GitFile)item).getFiles() : null;
        else if(item instanceof GitIndex) items = ((GitIndex)item).getEntry("/").getEntries();
        else if(item instanceof GitIndex.Entry)
            items = ((GitIndex.Entry)item).isDir()? ((GitIndex.Entry)item).getEntries() : null;
        return items;
    }

    /** Returns the text to be used for given item. */
    public String getText(Object anItem)
    {
        String value = null; if(anItem==null) return null;
        if(anItem instanceof GitBranch) value = ((GitBranch)anItem).getPlainName();
        else if(anItem instanceof GitCommit) { Date date = new Date(((GitCommit)anItem).getCommitTime());
            value = "Commit " + _fmt.format(date); }
        else if(anItem instanceof GitFile) value = ((GitFile)anItem).getName();
        else if(anItem instanceof GitIndex) value = "Index";
        else if(anItem instanceof GitIndex.Entry) value = ((GitIndex.Entry)anItem).getName();
        else if(anItem instanceof GitRef) value = ((GitRef)anItem).getName();
        return value;
    }
}

// Utility method.
private static Object[] add(Object anAry[], Object item)
{
    Object ary[] = new Object[anAry.length+1]; for(int i=0;i<anAry.length; i++) ary[i] = anAry[i];
    ary[ary.length-1] = item;
    return ary;
}

// DateFormat for commit labels.
static SimpleDateFormat _fmt = new SimpleDateFormat("M/d/yy hh:mm a");

}