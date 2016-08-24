package snap.app;
import java.util.*;
import snap.gfx.*;
import snap.util.*;
import snap.view.*;
import snap.viewx.*;
import snap.web.*;

/**
 * ToolBar.
 */
public class AppPaneToolBar extends ViewOwner {

    // The AppPane
    AppPane                  _appPane;
    
    // The file tabs pane
    HBox                     _fileTabsPane;
    
    // A list of open files
    List <WebFile>           _openFiles = new ArrayList();
    
    // The currently selected file
    WebFile                  _selectedFile;
    
    // The view for the currently selected view
    View                     _selectedView;
    
    // The SearchText
    TextField                _searchText;
    
    // The SearchMenu that shows suggestions for the SearchText
    PopupList <WebFile>      _searchMenu;
    
    // A placeholder for fill from toolbar button under mouse
    Paint                    _tempFill;
    
    // Constant for file tab attributes
    static Font              TAB_FONT = new Font("Arial Bold", 12);
    static Color             TAB_COLOR = new Color(.5,.65,.8,.8);
    static Color             TAB_COLOR_OVER = new Color(.9,.95,1.0,.8);
    static Color             TAB_BORDER_COLOR = new Color(.33,.33,.33,.66);
    static Border            TAB_BORDER = Border.createLineBorder(TAB_BORDER_COLOR,1);
    static Border            TAB_CLOSE_BORDER1 = Border.createLineBorder(Color.BLACK,.5);
    static Border            TAB_CLOSE_BORDER2 = Border.createLineBorder(Color.BLACK,1);
    
/**
 * Creates a new AppPaneToolBar.
 */
public AppPaneToolBar(AppPane anAppPane)  { _appPane = anAppPane; }

/**
 * Returns the AppPane.
 */
public AppPane getAppPane()  { return _appPane; }

/**
 * Returns the SelectedSite.
 */
public WebSite getSelectedSite()  { return getAppPane().getSelectedSite(); }

/**
 * Notification that a file was opened/selected by AppPane.
 */
public void setSelectedFile(WebFile aFile)
{
    // Set selected file
    if(aFile==_selectedFile) return;
    _selectedFile = aFile;
    
    // Add to OpenFiles
    addOpenFile(_selectedFile);
    buildFileTabs();
}

/**
 * Returns whether a file is an "OpenFile" (whether it needs a File Bookmark).
 */
protected boolean isOpenFile(WebFile aFile)
{
    if(aFile.isDir()) return false; // No directories
    return getAppPane().getSites().contains(aFile.getSite()) || aFile.getType().equals("java");
}

/**
 * Adds a file to OpenFiles list.
 */
public void addOpenFile(WebFile aFile)
{
    if(aFile==null || !isOpenFile(aFile)) return;
    if(ListUtils.containsId(_openFiles, aFile)) return;
    _openFiles.add(aFile);
}

/**
 * Removes a file from OpenFiles list.
 */
public int removeOpenFile(WebFile aFile)
{
    // Remove file from list (just return if not available)
    int index = ListUtils.indexOfId(_openFiles, aFile); if(index<0) return index;
    _openFiles.remove(index);
    
    // If removed file is selected file, set browser file to last file (that is still in OpenFiles list)
    if(aFile==_selectedFile) {
        WebURL url = getFallbackURL();
        if(!url.equals(getAppPane().getHomePageURL()))
            getAppPane().getBrowser().setTransition(WebBrowser.Instant);
        getAppPane().getBrowser().setURL(url);
    }
    
    // Rebuild file tabs and return
    buildFileTabs();
    return index;
}

/**
 * Returns the URL to fallback on when open file is closed.
 */
private WebURL getFallbackURL()
{
    // Return the most recently opened of the remaining OpenFiles, or the Project.HomePageURL
    AppPane appPane = getAppPane(); AppBrowser browser = appPane.getBrowser();
    WebURL urls[] = browser.getHistory().getNextURLs();
    for(WebURL url : urls) { WebFile file = url.getFile();
        if(_openFiles.contains(file))
            return url.getFileURL(); }
    urls = browser.getHistory().getLastURLs();
    for(WebURL url : urls) { WebFile file = url.getFile();
        if(_openFiles.contains(file))
            return url.getFileURL(); }
    return getAppPane().getHomePageURL();
}

/**
 * Selects the search text.
 */
public void selectSearchText()  { runLater(() -> _searchText.requestFocus()); }

/**
 * Override to add menu button.
 */
protected View createUI()
{
    // Do normal version
    SpringView uin = (SpringView)super.createUI();
    
    // Add MenuButton
    MenuButton menuButton = new MenuButton(); menuButton.setName("RunMenuButton");
    menuButton.setBounds(210,24,18,16); menuButton.setMinSize(18,16); menuButton.setPrefSize(18,16);
    menuButton.setItems(getRunMenuButtonItems());
    uin.addChild(menuButton);
    
    // Add FileTabsPane pane
    _fileTabsPane = new HBox(); _fileTabsPane.setSpacing(4); _fileTabsPane.setPadding(4,0,0,4);
    _fileTabsPane.setBounds(0,45,uin.getWidth(),24); _fileTabsPane.setAutosizing("-~-,--~");
    uin.addChild(_fileTabsPane);
    buildFileTabs();
    
    // Set min height and return
    uin.setMinHeight(uin.getHeight());
    return uin;
}

/**
 * Override to set PickOnBounds.
 */
protected void initUI()
{
    // Fix buttons to PickOnBounds (instead of paths) for HomeButton, BackButton, NextButton, RefreshButton, RunButton
    //getNative("HomeButton", javafx.scene.Node.class).setPickOnBounds(true);
    
    // Get SearchText and Enable KeyReleased
    _searchText = getView("SearchText", TextField.class);
    _searchText.setPromptText("Search"); _searchText.getLabel().setImage(Image.get(TextPane.class, "Find.png"));
    enableEvents(_searchText, KeyReleased);
    
    // Enable events on buttons
    String bnames[] = { "HomeButton", "BackButton", "NextButton", "RefreshButton", "RunButton" };
    for(String name : bnames) enableEvents(name, MouseReleased, MouseEntered, MouseExited);
}

/**
 * Respond to UI changes.
 */
public void respondUI(ViewEvent anEvent)
{
    // Get AppPane
    AppPane appPane = getAppPane();
    
    // Make buttons glow
    if(anEvent.isMouseEntered() && anEvent.getView()!=_selectedView) { View view = anEvent.getView();
        _tempFill = view.getFill(); view.setFill(Color.WHITE); return; }
    if(anEvent.isMouseExited() && anEvent.getView()!=_selectedView) { View view = anEvent.getView();
        view.setFill(_tempFill); return; }
    
    // Handle HomeButton
    if(anEvent.equals("HomeButton") && anEvent.isMouseReleased())
        appPane.showHomePage();
    
    // Handle LastButton, NextButton
    if(anEvent.equals("BackButton") && anEvent.isMouseReleased())
        appPane.getBrowser().trackBack();
    if(anEvent.equals("NextButton") && anEvent.isMouseReleased())
        appPane.getBrowser().trackForward();
    
    // Handle RefreshButton
    if(anEvent.equals("RefreshButton") && anEvent.isMouseReleased())
        appPane.getBrowser().reloadPage();
    
    // Handle RunButton
    if(anEvent.equals("RunButton") && anEvent.isMouseReleased())
        appPane._filesPane.run();
    
    // Handle RunConfigMenuItems
    if(anEvent.getName().endsWith("RunConfigMenuItem")) {
        String name = anEvent.getName().replace("RunConfigMenuItem", "");
        RunConfigs rconfs = RunConfigs.get(getSelectedSite());
        RunConfig rconf = rconfs.getRunConfig(name);
        if(rconf!=null) {
            rconfs.getRunConfigs().remove(rconf);
            rconfs.getRunConfigs().add(0, rconf);
            rconfs.writeFile();
            getAppPane().getToolBar().setRunMenuButtonItems();
            appPane._filesPane.run();
        }
    }
    
    // Handle RunConfigsMenuItem
    if(anEvent.equals("RunConfigsMenuItem"))
        appPane.getBrowser().setURL(WebURL.getURL(RunConfigsPage.class));
    
    // Handle FileTab
    if(anEvent.equals("FileTab") && anEvent.isMouseReleased())
        handleFileTabClicked(anEvent);

    // Handle SearchText
    if(anEvent.equals("SearchText"))
        handleSearchText(anEvent);
}

/**
 * Handle FileTab clicked.
 */
protected void handleFileTabClicked(ViewEvent anEvent)
{
    FileTab fileTab = anEvent.getView(FileTab.class);
    WebFile file = fileTab.getFile();
    
    // Handle single click
    if(anEvent.getClickCount()==1) {
        getAppPane().getBrowser().setTransition(WebBrowser.Instant);
        getAppPane().setSelectedFile(file);
    }
    
    // Handle double click
    else if(anEvent.getClickCount()==2) {
        WebBrowserPane bpane = new WebBrowserPane();
        bpane.getBrowser().setURL(file.getURL());
        bpane.getWindow().setVisible(true);
    }
}

/**
 * Creates a pop-up menu for preview edit button (currently with look and feel options).
 */
private List <MenuItem> getRunMenuButtonItems()
{
    // Create MenuItems list
    List <MenuItem> items = new ArrayList(); MenuItem mi;
    
    // Add RunConfigs MenuItems
    List <RunConfig> rconfs = RunConfigs.get(getSelectedSite()).getRunConfigs();
    for(RunConfig rconf : rconfs) { String name = rconf.getName();
        mi = new MenuItem(); mi.setName(name + "RunConfigMenuItem"); mi.setText(name); items.add(mi); }
    if(rconfs.size()>0) items.add(new MenuItem()); //new SeparatorMenuItem()
    
    // Add RunConfigsMenuItem
    mi = new MenuItem(); mi.setText("Run Configurations..."); mi.setName("RunConfigsMenuItem"); items.add(mi);
    
    // Return MenuItems
    return items;
}

/**
 * Sets the RunMenuButton items.
 */
public void setRunMenuButtonItems()
{
    MenuButton rmb = getView("RunMenuButton", MenuButton.class);
    rmb.setItems(getRunMenuButtonItems());
    for(MenuItem mi : rmb.getItems()) initUI(mi);
}

/**
 * Builds the file tabs.
 */
public void buildFileTabs()
{
    // If not on event thread, come back on that
    if(!isEventThread()) { runLater(() -> buildFileTabs()); return; }
    
    // Clear selected view
    _selectedView = null;
    
    // Create HBox for tabs
    HBox hbox = new HBox(); hbox.setSpacing(2);
    
    // Iterate over OpenFiles, create FileTabs, init and add
    for(WebFile file : _openFiles) {
        Label bm = new FileTab(file);
        initUI(bm); enableEvents(bm, MouseEvents);
        hbox.addChild(bm);
    }
    
    // Add box and call scaleFileTabs to scale them if needed
    _fileTabsPane.setChildren(hbox);
    scaleFileTabs();
}

/**
 * Scales file tabs HBox so all tabs show.
 */
private void scaleFileTabs()
{
    HBox hbox = (HBox)_fileTabsPane.getChild(0);
    double width1 = hbox.getPrefWidth(), width2 = _fileTabsPane.getWidth();
    if(width1>width2-4) {
        hbox.setScaleX((width2-4)/width1);
        hbox.setTransX(-(width1 - width2 + 4)/2);
    }
}

/**
 * Handle SearchText changes.
 */
public void handleSearchText(ViewEvent anEvent)
{
    // Get search menu
    PopupList <WebFile> searchMenu = getSearchMenu();
    
    // Handle KeyReleased: Let key do normal processing and call handleSearchTextKeyFinished()
    if(anEvent.isKeyReleased() && !anEvent.isEnterKey())
        runLater(() -> handleSearchTextKeyFinished(anEvent));
    
    // Handle ActionEvent
    else {
        searchMenu.hide();
        WebFile file = searchMenu.getSelectedItem();
        if(file!=null) {
            getAppPane().getBrowser().setFile(file);
            setViewValue(_searchText, "");
        }
        else if(getViewStringValue(_searchText)!=null && getViewStringValue(_searchText).length()>0) {
            String text = getViewStringValue(_searchText);
            int colon = text.indexOf(':');
            if(colon>0 && colon<6) {
                WebURL url = WebURL.getURL(text);
                getAppPane().getBrowser().setURL(url);
            }
            else {
                getAppPane().getSearchPane().search(getViewStringValue(_searchText));
                getAppPane().setSupportTrayIndex(SupportTray.SEARCH_PANE);
            }
            setViewValue(_searchText, "");
        }
    }
}

/**
 * Called after SearchText KeyReleased is processed.
 */
private void handleSearchTextKeyFinished(ViewEvent anEvent)
{
    PopupList <WebFile> searchMenu = getSearchMenu();
    if(anEvent.isDownArrow() || anEvent.isUpArrow()) return;
    if(anEvent.isEscapeKey()) setViewValue(_searchText, "");
    String prefix = anEvent.getStringValue(); if(prefix.equals("nulltest")) searchMenu = null;
    List <WebFile> files = getFilesForPrefix(anEvent.getStringValue());
    searchMenu.setItems(files);
    if(files.size()>0)
        searchMenu.setSelectedIndex(0);
    if(files.size()==0) searchMenu.hide();
    else if(!searchMenu.isShowing()) searchMenu.show(_searchText, 0, _searchText.getHeight());
}

/**
 * Returns the SearchMenu.
 */
public PopupList <WebFile> getSearchMenu()
{
    if(_searchMenu==null) {
        _searchMenu = new PopupList <WebFile>() {
            public void configureCell(snap.view.ListCell<WebFile> aCell) {
                super.configureCell(aCell);
                WebFile file = aCell.getItem(); if(file==null) return;
                aCell.setText(file.getName() + " - " + file.getParent().getPath());
            }
            public void fireActionEvent()  { super.fireActionEvent(); sendEvent(_searchText); }
        };
        _searchMenu.setRowHeight(22); _searchMenu.setPrefWidth(300);
    }
    return _searchMenu;
}

/**
 * Returns a list of files for given prefix.
 */
private List <WebFile> getFilesForPrefix(String aPrefix)
{
    List <WebFile> files = new ArrayList(); if(aPrefix==null || aPrefix.length()==0) return files;
    for(WebSite site : getAppPane().getSites())
        getFilesForPrefix(aPrefix, site.getRootDir(), files);
    Collections.sort(files, _fileComparator);
    return files;
}

/**
 * Gets files for given name prefix.
 */
private void getFilesForPrefix(String aPrefix, WebFile aFile, List <WebFile> theFiles)
{
    // If hidden file, just return
    SitePane spane = SitePane.get(aFile.getSite()); if(spane.isHiddenFile(aFile)) return;

    // If directory, recurse
    if(aFile.isDir()) for(WebFile file : aFile.getFiles())
        getFilesForPrefix(aPrefix, file, theFiles);
        
    // If file that starts with prefix, add to files
    else if(StringUtils.startsWithIC(aFile.getName(), aPrefix))
        theFiles.add(aFile);
}

/**
 * Comparator for files.
 */
Comparator<WebFile> _fileComparator = new Comparator<WebFile>() {
    public int compare(WebFile o1, WebFile o2) {
        int c = o1.getSimpleName().compareToIgnoreCase(o2.getSimpleName());
        return c!=0? c : o1.getName().compareToIgnoreCase(o2.getName());
    }
};

/**
 * A class to represent a rounded label.
 */
protected class FileTab extends Label {
    
    // The File
    WebFile  _file;
    
    /** Creates a new FileTab for given file. */
    public FileTab(WebFile aFile)
    {
        // Create label for file and configure
        _file = aFile;
        setText(aFile.getName()); setFont(TAB_FONT); setName("FileTab");
        setPrefHeight(19); setMaxHeight(19); setBorder(TAB_BORDER); setPadding(1,2,1,4);
        setFill(aFile==_selectedFile? Color.WHITE : TAB_COLOR);
        if(aFile==_selectedFile) _selectedView = this;
        
        // Add a close box graphic
        Polygon poly = new Polygon(0,2,2,0,5,3,8,0,10,2,7,5,10,8,8,10,5,7,2,10,0,8,3,5);
        ShapeView sview = new ShapeView(poly); sview.setBorder(TAB_CLOSE_BORDER1); sview.setPrefSize(11,11);
        sview.addEventHandler(e->handleTabCloseBoxEvent(e), MouseEntered, MouseExited, MouseReleased);
        setGraphicAfter(sview);
    }
    
    /** Returns the file. */
    public WebFile getFile()  { return _file; }
    
    /** Called for events on tab close button. */
    private void handleTabCloseBoxEvent(ViewEvent anEvent)
    {
        View cbox = anEvent.getView();
        if(anEvent.isMouseEntered()) { cbox.setFill(Color.CRIMSON); cbox.setBorder(TAB_CLOSE_BORDER2); }
        else if(anEvent.isMouseExited()) { cbox.setFill(null); cbox.setBorder(TAB_CLOSE_BORDER1); }
        else if(anEvent.isMouseReleased()) removeOpenFile(_file);
        anEvent.consume();
    }

    /** Returns bounds shape as rounded rect. */
    public Shape getBoundsShape()  { return new RoundRect(0,0,getWidth(),getHeight(),6); }
}

}