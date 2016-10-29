package snap.app;
import java.util.*;
import snap.gfx.*;
import snap.javaparse.*;
import snap.project.Project;
import snap.view.*;
import snap.web.*;

/**
 * A custom class.
 */
public class SearchPane extends ViewOwner {
    
    // The app pane
    AppPane         _appPane;
    
    // The current search
    Search          _search;
    
    // The current selected result
    Result          _sresult;

/**
 * Creates a new search pane for app pane.
 */
public SearchPane(AppPane anAppPane)  { _appPane = anAppPane; }

/**
 * Returns the root project.
 */
public Project getRootProject()  { return Project.get(_appPane.getRootSite()); }

/**
 * Returns the current search.
 */
public Search getSearch()  { return _search; }

/**
 * Returns the current search results.
 */
public List <Result> getResults()  { return _search!=null? _search._results : null; }

/**
 * Returns the current selected search result.
 */
public Result getSelectedResult()  { return _sresult; }

/**
 * Sets the current selected search result.
 */
public void setSelectedResult(Result aResult)  { _sresult = aResult; }

/**
 * Initialize UI.
 */
protected void initUI()
{
    // Configure ResultsList
    ListView <Result> resultsList = getView("ResultsList", ListView.class);
    resultsList.setCellConfigure(this::configureResultListCell);
    resultsList.setRowHeight(24);
}

/**
 * Reset UI.
 */
public void resetUI()
{
    String results = "";
    if(_search!=null) {
        int hits = 0; for(Result rslt : _search._results) hits += rslt._count;
        String typ = _search._kind==Search.Kind.Declaration? "declarations" : "references";
        results = String.format("'%s' - %d %s", _search._string, hits, typ);
    }
    setViewValue("SearchResultsText", results);
}

/**
 * Respond to UI changes.
 */
public void respondUI(ViewEvent anEvent)
{
    // Handle SearchButton
    if(anEvent.equals("SearchButton")) {
        DialogBox dbox = new DialogBox("Search Panel"); dbox.setMessage("Enter search string:");
        String string = dbox.showInputDialog(getUI(), null);
        if(string!=null)
            search(string);
    }
    
    // Handle ClearButton
    if(anEvent.equals("ClearButton"))
        _search = null;
    
    // Handle ResultsList
    if(anEvent.equals("ResultsList")) {
        Result result = getSelectedResult();
        _appPane.getBrowser().setURLString(result.getURLString());
    }
}

/**
 * Search for given term.
 */
public void search(String aString)
{
    _search = new Search(); _search._string = aString;
    for(WebSite site : _appPane.getSites())
        search(site.getRootDir(), _search._results, aString.toLowerCase());
    resetLater();
}

/**
 * Search for given string in given file with list for results.
 */
protected void search(WebFile aFile, List <Result> theResults, String aString)
{
    // If hidden file, just return
    SitePane spane = SitePane.get(aFile.getSite()); if(spane.isHiddenFile(aFile)) return;
    
    // Handle directory
    if(aFile.isDir()) {
        if(aFile==_appPane.getBuildDir()) return;
        for(WebFile file : aFile.getFiles())
            search(file, theResults, aString);
    }
    
    // Handle JavaFile
    else if(isSearchTextFile(aFile)) {
        String text = aFile.getText().toLowerCase(); Result result = null; int len = aString.length();
        for(int start=text.indexOf(aString); start>=0; start=text.indexOf(aString, start+len)) {
            if(result==null) theResults.add(result=new Result(aFile));
            else result._count++;
        }
    }
}

/**
 * Returns whether file is to be included in search text.
 */
protected boolean isSearchTextFile(WebFile aFile)
{
    String type = aFile.getType();
    return type.equals("java") || type.equals("snp") || type.equals("rib") || type.equals("txt");
}

/**
 * Search for given element reference.
 */
public void searchReference(JNode aNode)
{
    JavaDecl decl = aNode.getDecl(); if(decl==null) { beep(); return; }
    _search = new Search(); _search._string = decl.getMatchName(); _search._kind = Search.Kind.Reference;
    for(WebSite site : _appPane.getSites())
        searchReference(site.getRootDir(), _search._results, decl);
    resetLater();
}
    
/**
 * Search for given element reference.
 */
public void searchReference(WebFile aFile, List <Result> theResults, JavaDecl aDecl)
{
    // If hidden file, just return
    SitePane spane = SitePane.get(aFile.getSite()); if(spane.isHiddenFile(aFile)) return;
    
    // Handle directory
    if(aFile.isDir()) {
        if(aFile==_appPane.getBuildDir()) return;
        for(WebFile file : aFile.getFiles())
            searchReference(file, theResults, aDecl);
    }
    
    // Handle JavaFile
    else if(aFile.getType().equals("java")) {
        Project proj = getRootProject();
        JavaData jdata = JavaData.get(aFile);
        Set <JavaDecl> refs = jdata.getRefs();
        for(JavaDecl decl : refs) {
            if(decl.matches(proj, aDecl)) {
                List <JNode> nodes = new ArrayList(); JavaDecl.getRefMatches(proj, jdata.getJFile(), aDecl, nodes);
                for(JNode node : nodes)
                    theResults.add(new Result(node));
                return;
            }
        }
    }
}

/**
 * Search for given element reference.
 */
public void searchDeclaration(JNode aNode)
{
    JavaDecl decl = aNode.getDecl(); if(decl==null) { beep(); return; }
    _search = new Search(); _search._string = decl.getMatchName(); _search._kind = Search.Kind.Declaration;
    for(WebSite site : _appPane.getSites())
        searchDeclaration(site.getRootDir(), _search._results, decl);
    resetLater();
}
    
/**
 * Search for given element reference.
 */
public void searchDeclaration(WebFile aFile, List <Result> theResults, JavaDecl aDecl)
{
    // If hidden file, just return
    SitePane spane = SitePane.get(aFile.getSite()); if(spane.isHiddenFile(aFile)) return;
    
    // Handle directory
    if(aFile.isDir()) {
        if(aFile==_appPane.getBuildDir()) return;
        for(WebFile file : aFile.getFiles())
            searchDeclaration(file, theResults, aDecl);
    }
    
    // Handle JavaFile
    else if(aFile.getType().equals("java")) {
        Project proj = getRootProject();
        JavaData jdata = JavaData.get(aFile);
        for(JavaDecl decl : jdata.getDecls())
            if(decl.matches(proj, aDecl)) {
                List <JNode> nodes = new ArrayList(); JavaDecl.getDeclMatches(proj, jdata.getJFile(), aDecl, nodes);
                for(JNode node : nodes)
                    theResults.add(new Result(node));
                return;
            }
    }
}

/**
 * A class to hold a search.
 */
public static class Search {

    // The search string
    String          _string;
    
    // The kind
    Kind            _kind = Kind.Text;
    
    // The results
    List <Result>   _results = new ArrayList();
    
    // Constants for kind
    public enum Kind { Text, Reference, Declaration }
}

/**
 * A class to hold a search result.
 */
public class Result {

    // The file
    WebFile       _file;
    
    // The JNode
    JNode         _node;
    
    // The match count
    int           _count = 1;
    
    /** Creates a new result. */
    public Result(WebFile aFile)  { _file = aFile; }
    
    /** Creates a new result. */
    public Result(JNode aNode)  { _node = aNode; _file = _node.getFile().getSourceFile(); }
    
    /** Standard toString implementation. */
    public String getDescriptor()
    {
        JavaDecl decl = _node!=null? _node.isDecl()? _node.getDecl() : _node.getEnclosingDecl() : null;
        if(decl!=null) return decl.getPrettyName();
        String s = _file.getName() + " - " + _file.getParent().getPath();
        s += " (" + _count + " match" + (_count==1? "" : "es") + ")";
        return s;
    }
    
    /** Returns an image. */
    public Image getImage()
    {
        JavaDecl decl = _node!=null? _node.isDecl()? _node.getDecl() : _node.getEnclosingDecl() : null;
        if(decl==null) return ViewUtils.getFileIconImage(_file);
        if(decl.isClass()) return snap.javatext.JavaTextBox.ClassImage;
        if(decl.isField()) return snap.javatext.JavaTextBox.FieldImage;
        return snap.javatext.JavaTextBox.MethodImage;
    }
    
    /** Returns a URL for result. */
    public String getURLString()
    {
        String urls = _file.getURL().getString();
        if(_search._kind==Search.Kind.Text) urls += "#Find=" + _search._string;
        else if(_node!=null) urls += String.format("#Sel=%d-%d", _node.getStart(), _node.getEnd());
        return urls;
    }
}

/**
 * Called to configure cell.
 */
private void configureResultListCell(ListCell <Result> aCell)
{
    Result result = aCell.getItem(); if(result==null) return;
    aCell.setText(result.getDescriptor());
    aCell.setImage(result.getImage()); aCell.getGraphic().setPadding(2,4,2,4);
}

}