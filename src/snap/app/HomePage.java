package snap.app;
import snap.util.StringUtils;
import snap.view.*;
import snap.viewx.WebPage;
import snap.web.*;

/**
 * A custom class.
 */
public class HomePage extends WebPage {

/**
 * Returns the browser as AppBrowser.
 */
public AppBrowser getBrowser()  { return (AppBrowser)super.getBrowser(); }

/**
 * Returns the AppPane RootSite.
 */
public WebSite getRootSite()  { return getBrowser().getAppPane().getRootSite(); }

/**
 * Override to put in Page pane.
 */
protected View createUI()  { return  new ScrollView(super.createUI()); }

/**
 * Initialize UI.
 */
public void initUI()
{
    enableEvents("MakeStarterProject", MouseEvents);
    enableEvents("WatchVideos", MouseEvents);
    enableEvents("CreateFile", MouseEvents);
    enableEvents("NewJavaFXFile", MouseEvents);
    enableEvents("NewDataTable", MouseEvents);
    enableEvents("NewReport", MouseEvents);
    enableEvents("SnapSchool", MouseEvents);
    enableEvents("ServerSettings", MouseEvents);
}

/**
 * RespondUI.
 */
public void respondUI(ViewEvent anEvent)
{
    // Register for animation on MouseEntered and MouseExited
    //if(anEvent.isMouseEntered()) getAnimator(anEvent.getView()).scaleTo(1.15,1.15,200).play();
    //if(anEvent.isMouseExited() || anEvent.isMouseClicked()) getAnimator(anEvent.getView()).scaleTo(1,1,200).play();
    if(anEvent.isMouseEntered()) new Anim(anEvent.getView(), "Scale", null, 1.15, 200).play();
    if(anEvent.isMouseExited() || anEvent.isMouseClicked()) new Anim(anEvent.getView(), "Scale",null,1,200).play();
    
    // Handle WatchVideos
    if(anEvent.equals("WatchVideos") && anEvent.isMouseClicked())
        getBrowser().setURLString("http://www.reportmill.com/snap/gallery/AddressBook/index.html");

    // Handle CreateFile
    if(anEvent.equals("CreateFile") && anEvent.isMouseClicked())
        getBrowser().getAppPane().showNewFilePanel();
    
    // Handle MakeStarterProject
    if(anEvent.equals("MakeStarterProject") && anEvent.isMouseClicked()) {
        WebFile studioFile = makeStudioProject(getRootSite());
        getBrowser().setFile(studioFile);
    }

    // Handle NewJavaFile
    if(anEvent.equals("NewJavaFile") && anEvent.isMouseClicked()) {
        WebFile file = getRootSite().createFile("/Untitled.java", false);
        WebPage page = getBrowser().createPage(file);
        file = page.showNewFilePanel(getBrowser());
        if(file!=null) try {
            configureJavaStarterFile(file);
            file.save();
            getBrowser().setFile(file);
        }
        catch(Exception e) { getBrowser().showException(file.getURL(), e); }
    }
    
    // Handle NewJavaFXFile
    if(anEvent.equals("NewJavaFXFile") && anEvent.isMouseClicked()) {
        WebFile file = getRootSite().createFile("/Untitled.snp", false);
        WebPage page = getBrowser().createPage(file);
        file = page.showNewFilePanel(getBrowser());
        if(file!=null) try {
            file.save();
            getBrowser().setFile(file);
        }
        catch(Exception e) { getBrowser().showException(file.getURL(), e); }
    }
    
    // Handle NewDataTable
    if(anEvent.equals("NewDataTable") && anEvent.isMouseClicked()) {
        WebFile file = getRootSite().createFile("/Untitled.table", false);
        WebPage page = getBrowser().createPage(file);
        file = page.showNewFilePanel(getBrowser());
        if(file!=null) try {
            file.save();
            getBrowser().setFile(file);
        }
        catch(Exception e) { getBrowser().showException(file.getURL(), e); }
    }
    
    // Handle NewReport
    if(anEvent.equals("NewReport") && anEvent.isMouseClicked()) {
        WebFile file = getRootSite().createFile("/Untitled.rpt", false);
        WebPage page = getBrowser().createPage(file);
        file = page.showNewFilePanel(getBrowser());
        if(file!=null) try {
            file.save();
            getBrowser().setFile(file);
        }
        catch(Exception e) { getBrowser().showException(file.getURL(), e); }
    }
    
    // Handle SnapSchool
    if(anEvent.equals("SnapSchool") && anEvent.isMouseClicked())
        getBrowser().setURLString("http://reportmill.com/snap/school/SnapSchool.rpt");
    
    // Handle ProjectSettings
    //if(anEvent.equals("ProjectSettings") && anEvent.isMouseClicked())
    //    getBrowser().setFile(getProject().getRootDirectory());
    
    // Handle ServerSettings
    if(anEvent.equals("ServerSettings") && anEvent.isMouseClicked())
        getBrowser().setURLString("http://www.reportmill.com/support/UserGuide.pdf");
}

/**
 * Makes given site a Studio project.
 */
public static WebFile makeStudioProject(WebSite aSite)
{
    WebFile studioFile = StudioFilesPane.makeStudioProject(aSite);
    SitePane spane = SitePane.get(aSite);
    spane.setUseSnapEditor(true);
    spane.setHomePageURL(studioFile.getURL());
    RunConfigs rc = RunConfigs.get(aSite); if(rc.getRunConfig()==null) {
        rc.getRunConfigs().add(new RunConfig().setName("StudioApp").setMainClassName("Scene1"));
        rc.writeFile();
    }
    return studioFile;
}

/**
 * Override to configure new file.
 */
public static void configureJavaStarterFile(WebFile aFile)
{
    StringBuffer sb = new StringBuffer();
    sb.append("import snap.viewx.*;\n\n");
    sb.append("/**\n * A custom class.\n */\n");
    sb.append("public class ").append(aFile.getSimpleName()).append(" extends SnapScene {\n\n");
    sb.append("/**\n * Enter your custom code here!\n */\n");
    sb.append("public void main()\n{\n");
    sb.append("    println(\"Change this\");\n").append("}\n\n");
    sb.append("}");
    aFile.setBytes(StringUtils.getBytes(sb.toString()));
}

/**
 * Return better title.
 */
public String getTitle()  { return "Home Page"; }

}