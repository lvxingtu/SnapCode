package snap.app;
import java.text.SimpleDateFormat;
import java.util.*;
import snap.project.*;
import snap.project.GitDir.*;
import snap.util.*;
import snap.view.*;
import snap.viewx.WebBrowser;
import snap.web.*;

/**
 * A custom class.
 */
public class VcsPaneGit extends VcsPane {
    
    // The main SplitView
    SplitView          _topSplit;
    
    // The HBox
    HBox               _hbox;
    
    // ListViews
    List <ListView>    _lists = new ArrayList();
    
    // The VersionControlGit
    VersionControlGit  _vcg;
    
    // The GitDir
    GitDir             _gdir;

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
    addListView(null, getRootItems());
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
    _topSplit = new SplitView(); _topSplit.setVertical(true); _topSplit.setAutosizing("-~-,-~~");
    _topSplit.setBounds(5,5,pane.getWidth()-10, pane.getHeight()-10);
    pane.addChild(_topSplit);
    
    // Add Horizontal browser UI
    _hbox = new HBox(); _hbox.setPrefHeight(300); _hbox.setFillHeight(true);
    ScrollView spane = new ScrollView(_hbox); spane.setPrefHeight(300); spane.setFillHeight(true);
    
    // Add content view and return UI
    _topSplit.addItem(spane);
    return superUI;
}

/**
 * Initialize UI.
 */
protected void initUI()
{
    addListView(null, getRootItems());
    
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
    // Handle ListView
    if(anEvent.equals("ListView")) {
        Object item = anEvent.getSelectedItem(); Object items[] = null;
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
        if(items!=null)
            addListView(anEvent.getView(), items);
            
        if(item instanceof GitFile) { GitFile gf = (GitFile)item;
            GitCommit gc = gf.isFile()? getParent(GitCommit.class) : null;
            if(gc!=null) {
                WebSite site = gc.getSite();
                WebFile file = site.getFile(gf.getPath());
                WebBrowser browser = new WebBrowser();
                _topSplit.setItem(browser, 1);
                browser.setFile(file);
            }
        }
        else if(item instanceof GitIndex.Entry) { GitIndex.Entry gie = (GitIndex.Entry)item;
            WebSite site = _vcg.getGitDir().getIndexSite();
            WebFile file = site.getFile(gie.getPath());
            WebBrowser browser = new WebBrowser();
            _topSplit.setItem(browser, 1);
            browser.setFile(file);
        }
    }
    
    // Handle SyncButton
    else if(anEvent.equals("SyncButton"))
        getUpdateFiles(null);
    
    // Handle Update/Commit/Replace buttons
    else super.respondUI(anEvent);
}

public <T> T getParent(Class <T> aClass)
{
    List list = ArrayUtils.asArrayList(_hbox.getChildren()); Collections.reverse(list);
    for(ScrollView sp : (List<ScrollView>)list) { ListView ln = (ListView)sp.getContent();
        Object item = ln.getSelectedItem();
        if(aClass.isInstance(item))
            return (T)item; }
    return null;
}

/**
 * Adds a ListView to UI.
 */
protected void addListView(View aPrevNode, Object theItems[])
{
    // If previous node provided, remove those after it
    int i = ArrayUtils.indexOfId(_hbox.getChildren(), aPrevNode!=null? aPrevNode.getParent(ScrollView.class) : null);
    while(i+1<_hbox.getChildCount()) _hbox.removeChild(_hbox.getChildCount()-1);

    // Create list view and add
    ListView lview = new ListView(); lview.setName("ListView"); lview.setPrefWidth(200);
    lview.setCellConfigure(c -> configureListCell((ListCell)c));
    lview.setItems(theItems);
    
    ScrollView spane = new ScrollView(lview); spane.setShowVBar(true);
    _hbox.addChild(spane);
    lview.setOwner(this);
    
    // Make sure new ListView is visible
    runLaterDelayed(50, () -> spane.scrollToVisible(lview.getBoundsInside()));
}
    
protected Object[] getRootItems()
{
    List items = new ArrayList(); if(!_vc.getExists()) return new Object[0];
    items.add(_gdir.getHead());
    items.add(_gdir.getIndex());
    return items.toArray();
}

/**
 * Called to configure ListCell.
 */
private void configureListCell(ListCell aCell)
{
    // Evaluate key
    Object anItem = aCell.getItem(); if(anItem==null) return;
    String value = null;
    if(anItem instanceof GitBranch) value = ((GitBranch)anItem).getPlainName();
    else if(anItem instanceof GitCommit) { Date date = new Date(((GitCommit)anItem).getCommitTime());
        value = "Commit " + _fmt.format(date); }
    else if(anItem instanceof GitFile) value = ((GitFile)anItem).getName();
    else if(anItem instanceof GitIndex) value = "Index";
    else if(anItem instanceof GitIndex.Entry) value = ((GitIndex.Entry)anItem).getName();
    else if(anItem instanceof GitRef) value = ((GitRef)anItem).getName();
    aCell.setText(value);
}

// DateFormat for commit labels.
static SimpleDateFormat _fmt = new SimpleDateFormat("M/d/yy hh:mm a");

// Utility method.
private static Object[] add(Object anAry[], Object item)
{
    Object ary[] = new Object[anAry.length+1];
    for(int i=0;i<anAry.length; i++) ary[i] = anAry[i];
    ary[ary.length-1] = item;
    return ary;
}

}