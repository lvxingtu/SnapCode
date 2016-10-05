package snap.app;
import snap.gfx.*;
import snap.view.*;
import snap.viewx.WebPage;
import snap.web.*;

/**
 * A custom class.
 */
public class HomePage extends WebPage {
    
    // Whether to do stupid animation (rotate buttons on mouse enter)
    boolean       _stupidAnim;

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
    enableEvents("Header", MouseClicked);
    enableEvents("NewJavaFile", MouseEvents);
    enableEvents("NewSnapFile", MouseEvents);
    enableEvents("AddSnapKit", MouseEvents);
    enableEvents("AddSnapTea", MouseEvents);
    enableEvents("SnapDocs", MouseEvents);
    enableEvents("RMDocs", MouseEvents);
    enableEvents("NewReport", MouseEvents);
    enableEvents("RMUserGuide", MouseEvents);
}

/**
 * RespondUI.
 */
public void respondUI(ViewEvent anEvent)
{
    // Trigger animations on main buttons for MouseEntered/MouseExited
    if(anEvent.isMouseEntered()) {
        if(_stupidAnim) anEvent.getView().getAnimCleared(200).setScale(1.15).getRoot(1000).setRotate(180).play();
        else anEvent.getView().getAnimCleared(200).setScale(1.15).play(); }
    if(anEvent.isMouseExited()) {
        if(_stupidAnim) anEvent.getView().getAnimCleared(200).setScale(1).getRoot(1000).setRotate(0).play();
        else anEvent.getView().getAnimCleared(200).setScale(1).play(); }
        
    // Handle Header: Play click anim and toggle StupidAnim
    if(anEvent.equals("Header")) { View hdr = anEvent.getView(); _stupidAnim = !_stupidAnim;
        hdr.setBorder(_stupidAnim? Color.MAGENTA : Color.BLACK, 1);
        hdr.setScale(1.05); hdr.getAnimCleared(200).setScale(1).play();
    }

    // Handle NewJavaFile
    if(anEvent.equals("NewJavaFile") && anEvent.isMouseClicked())
        getBrowser().getAppPane().showNewFilePanel();
    
    // Handle NewSnapFile
    if(anEvent.equals("NewSnapFile") && anEvent.isMouseClicked()) {
        WebFile file = getRootSite().createFile("/Untitled.snp", false);
        WebPage page = getBrowser().createPage(file);
        file = page.showNewFilePanel(getBrowser());
        if(file!=null) try {
            file.save();
            getBrowser().setFile(file);
        }
        catch(Exception e) { getBrowser().showException(file.getURL(), e); }
    }
    
    // Handle AddSnapKit
    if(anEvent.equals("AddSnapKit") && anEvent.isMouseClicked()) {
        System.out.println("Add SnapKit");
    }

    // Handle AddSnapKit
    if(anEvent.equals("AddSnapTea") && anEvent.isMouseClicked()) {
        System.out.println("Add SnapTea");
    }

    // Handle SnapDocs
    if(anEvent.equals("SnapDocs") && anEvent.isMouseClicked())
        GFXEnv.getEnv().openURL("http://www.reportmill.com/snap1/javadoc");

    // Handle RMDocs
    if(anEvent.equals("RMDocs") && anEvent.isMouseClicked())
        GFXEnv.getEnv().openURL("http://www.reportmill.com/support");
    
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
    
    // Handle RMUserGuide
    if(anEvent.equals("RMUserGuide") && anEvent.isMouseClicked())
        getBrowser().setURLString("http://www.reportmill.com/support/UserGuide.pdf");
}

/** Makes given site a Studio project. */
/*public static WebFile makeStudioProject(WebSite aSite) {
    WebFile stdFile = StudioFilesPane.makeStudioProject(aSite);
    SitePane spane = SitePane.get(aSite); spane.setUseSnapEditor(true); spane.setHomePageURL(stdFile.getURL());
    RunConfigs rc = RunConfigs.get(aSite); if(rc.getRunConfig()==null) {
        rc.getRunConfigs().add(new RunConfig().setName("StudioApp").setMainClassName("Scene1")); rc.writeFile(); }
    return stdFile; }
/** Configure new SnapScene java file. */
//public static void configureJavaStarterFile(WebFile aFile) {
//    StringBuffer sb = new StringBuffer(); sb.append("import snap.viewx.*;\n\n");
//    sb.append("/**\n * A custom class.\n */\n");
//    sb.append("public class ").append(aFile.getSimpleName()).append(" extends SnapScene {\n\n");
//    sb.append("/**\n * Enter custom code here!\n */\n");
//    sb.append("public void main()\n{\n"); sb.append("    println(\"Change this\");\n").append("}\n\n");sb.append("}");
//    aFile.setBytes(StringUtils.getBytes(sb.toString())); }

/**
 * Return better title.
 */
public String getTitle()  { return "Home Page"; }

}