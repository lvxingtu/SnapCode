package snap.app;
import java.io.File;
import java.util.List;
import snap.gfx.*;
import snap.javaparse.*;
import snap.project.*;
import snap.util.*;
import snap.view.*;
import snap.viewx.*;
import snap.web.*;

/**
 * Visual editor for scene.
 */
public class StudioFilePage extends WebPage {

    // The Scene
    SnapScene        _scene;

    // Mouse drag variables
    SnapActor        _mactor; double _mx, _my;

/**
 * Returns the scene java file.
 */
public WebFile getJavaFile()  { return getSite().getFile("/Scene1.java"); }

/**
 * Returns the scene.
 */
public SnapScene getMainScene()  { return _scene!=null? _scene : (_scene=createMainScene()); }

/**
 * Creates the scene.
 */
protected SnapScene createMainScene()
{
    WebFile jfile = getJavaFile();
    Project proj = Project.get(jfile);
    WebFile cfile = proj.getClassFile(jfile);
    Class cls = proj.getClassForFile(cfile);
    SnapScene scene = null; try { scene = (SnapScene)cls.newInstance(); }
    catch(Throwable e) { System.err.println("StudioFilePage.createMainScene: " + e); return scene = new SnapScene(); }
    scene.setAutoStart(false);
    return scene;
}

/**
 * Resets the scene.
 */
public void resetMainScene()
{
    // Get new scene and install in frame
    StackView sceneFrame = getView("SceneFrame", StackView.class); _scene = null;
    sceneFrame.setChildren(getMainScene());

    // Turn on mouse events to move Scene objects and drag target events for adding cast members 
    enableEvents(_scene, MouseEvents); // _scene.getActorPane();
    enableEvents(_scene, DragEnter, DragOver, DragExit, DragDrop); // _scene.getActorPane()
    //initUI(_scene.getUI());
}

/**
 * Initialize UI.
 */
protected void initUI()
{
    runLater(() -> resetMainScene());
}

/**
 * Respond UI.
 */
public void respondUI(ViewEvent anEvent)
{
    // Handle MousePressed
    if(anEvent.isMousePress()) {
        _mx = anEvent.getX(); _my = anEvent.getY();
        _mactor = getMainScene().getActorAt(_mx, _my, null); if(_mactor==null) return;
        _mactor.setEffect(new ShadowEffect(8, Color.GRAY, 4, 4));
    }
    
    // Handle MouseDragged
    if(anEvent.isMouseDrag() && _mactor!=null) {
        double mx = anEvent.getX(), my = anEvent.getY();
        _mactor.setX(_mactor.getX() + mx - _mx); _mx = mx;
        _mactor.setY(_mactor.getY() + my - _my); _my = my;
    }
    
    // Handle MouseReleased
    if(anEvent.isMouseRelease() && _mactor!=null) {
        _mactor.setEffect(null);
        double nx = _mactor.getX(), nw = _mactor.getWidth(); 
        if(nx<-nw/2 || nx>_mactor.getParent().getWidth())
            removeActor(_mactor);
        else moveActor(_mactor); _mactor = null;
    }
    
    // Handle Drag any
    if(anEvent.isDragEvent()) { //System.out.println("DragEvent");
        anEvent.acceptDrag(); anEvent.consume(); }
    
    // Handle DragDrop: Get file, image and add actor
    if(anEvent.isDragDropEvent()) {
        if(!anEvent.hasDragFiles()) return; //anEvent.setDropCompleted(false);
        List <File> files = anEvent.getDragFiles();
        WebFile wfile = WebURL.getURL(files.get(0)).getFile(); double x = anEvent.getX(), y = anEvent.getY();
        runLaterDelayed(100, () -> addActor(wfile, x, y, true));
    }
    
    // Handle AddNewActorButton
    if(anEvent.equals("AddNewActorButton"))
        showAddActorPanel();
    
    // Handle ShowBackgroundButton
    if(anEvent.equals("ShowBackgroundButton")) {
        String fname = '/' + getMainScene().getClass().getSimpleName() + ".snp";
        WebFile file = getSite().getFile(fname);
        if(file==null) {
            file = getSite().createFile(fname, false);
            file.setBytes(getDocXML().getBytes());
            try { file.save(); } catch(Exception e) { throw new RuntimeException(e); }
        }
        getBrowser().setURL(file.getURL());
    }
    
    // Handle ResetSceneButton
    if(anEvent.equals("ResetSceneButton")) resetMainScene();
}

/**
 * Returns document xml.
 */
private XMLElement getDocXML()
{
    XMLElement xml = new XMLElement("document");
    XMLElement pxml = new XMLElement("page"); pxml.add("width", 720); pxml.add("height", 405); xml.add(pxml);
    return xml;
}

/**
 * Runs a panel to add actor.
 */
void showAddActorPanel()
{
    // Run FileChooser
    WebURL url1 = WebURL.getURL(getClass(), "pkg.images/Car.png");
    WebURL url2 = WebURL.getURL(getClass(), "pkg.images/Cat.png");
    WebURL url3 = WebURL.getURL(getClass(), "pkg.images/Dog.png");
    WebURL url4 = WebURL.getURL(getClass(), "pkg.images/Duke.png");
    ImageChooser fc = new ImageChooser(url1.getFile(), url2.getFile(), url3.getFile(), url4.getFile());
    WebFile file = fc.showDialogPanel(getUI(), "Select Actor Image", "Select Actor:");
    if(file!=null) runLater(() -> addActor(file, 360, 200, false));
}

/**
 * Adds an actor for given image file.
 */
void addActor(WebFile anImageFile, double aX, double aY, boolean doAskName)
{
    // Get safe name
    String name = StringUtils.toCamelCase(anImageFile.getSimpleName());
    if(doAskName) {
        DialogBox dbox = new DialogBox("Enter Class Name"); dbox.setQuestionMessage("Enter Class Name:");
        name = dbox.showInputDialog(getUI(), name); if(name==null) return;
        name = StringUtils.toCamelCase(name);
    }
    
    // Correct for duplicate name
    WebSite site = getSite();
    if(site.getFile("/" + name + ".java")!=null) {
        int i = 1; while(site.getFile("/" + name + i + ".java")!=null) i++; name = name + i; }
    
    // Save Image file
    WebFile ifile = anImageFile;
    if(anImageFile.getSite()!=getSite() || !anImageFile.getName().equals(name)) {
        String path = "/images/" + name + '.' + anImageFile.getType();
        ifile = site.createFile(path, false);
        ifile.setBytes(anImageFile.getBytes());
        try { ifile.save(); }
        catch(Exception e) { throw new RuntimeException(e); }
    }
    
    // Add actor
    addActor(name, Image.get(ifile), aX, aY);
}

/**
 * Adds an actor for name, image and location.
 */
void addActor(String aName, Image anImage, double aX, double aY)
{
    // Get Site and SceneFile
    String name = aName;
    WebSite site = getSite();
    
    // Create ActorFile text
    StringBuffer atext = new StringBuffer();
    atext.append("import snap.viewx.*;\n\n");
    atext.append("/**\n * An Actor implementation.\n */\n");
    atext.append("public class " + name + " extends SnapActor {\n\n");
    atext.append("/**\n * Initialize Actor here.\n */\n");
    atext.append("public " + name + "()\n{\n}\n\n");
    atext.append("/**\n * Initialize Actor here.\n */\n");
    atext.append("public void main()\n{\n}\n\n");
    atext.append("/**\n * Update Actor here.\n */\n");
    atext.append("public void act()\n{\n}\n\n}");
    
    // Set text and save file
    WebFile actorFile = site.createFile("/" + name + ".java", false);
    actorFile.setText(atext.toString());
    try { actorFile.save(); }
    catch(Exception e) { throw new RuntimeException(e); }
    
    // Configure SceneFile text and save
    WebFile sceneFile = getJavaFile();
    String stext = sceneFile.getText();
    JFile jfile = JavaData.get(sceneFile).getJFile();
    JMemberDecl constrDecl = jfile.getClassDecl().getConstructorDecl(jfile.getName(), null);
    if(constrDecl==null) constrDecl = jfile.getClassDecl().getMethodDecl("initScene", null);
    int x = (int)aX - (int)anImage.getWidth()/2, y = (int)aY - (int)anImage.getHeight()/2;
    String addActorStr = "    addActor(new " + name + "(), " + x + ", " + y + ");\n";
    stext = new StringBuffer(stext).insert(constrDecl.getEnd()-1, addActorStr).toString();
    sceneFile.setText(stext);
    try { sceneFile.save(); }
    catch(Exception e) { throw new RuntimeException(e); }
    
    // Reset Scene
    StudioFilesPane.waitForCompile(sceneFile); getBrowser().reloadFile(sceneFile);
    runLater(() -> resetMainScene());
}

/**
 * Moves an actor in file.
 */
void moveActor(SnapActor anActor)
{
    String name = anActor.getName();
    String loc = "addActor(new " + name + "()";
    
    // Get file, text and start of line of code to modify
    WebFile sceneFile = getJavaFile();
    String text = sceneFile.getText();
    int index = text.indexOf(loc); if(index<0) return;
    
    // Update line of code
    int index2 = text.indexOf(')', index + loc.length());
    loc = loc + ", " + (int)anActor.getX() + ", " + (int)anActor.getY();
    text = text.substring(0, index) + loc + text.substring(index2);
    
    // Replace text and save
    sceneFile.setText(text);
    try { sceneFile.save(); }
    catch(Exception e) { throw new RuntimeException(e); }
    
    // Wait for compile and reset scene
    StudioFilesPane.waitForCompile(sceneFile); getBrowser().reloadFile(sceneFile);
    runLater(() -> resetMainScene());
}

/**
 * Moves an actor in file.
 */
void removeActor(SnapActor anActor)
{
    // Get SceneFile and remove "addActor()" line of code
    WebFile sceneFile = getJavaFile();
    String text = sceneFile.getText();
    final String name = anActor.getName();
    String lineOfCode = "addActor(new " + name + "()";
    int index = text.indexOf(lineOfCode); if(index<0) return;
    int index2 = text.indexOf('\n', index + lineOfCode.length()); index2++;
    while(text.charAt(index-1)!='\n') index--;
    text = text.substring(0, index) + text.substring(index2);
    
    // Replace text and save
    sceneFile.setText(text);
    try { sceneFile.save(); }
    catch(Exception e) { throw new RuntimeException(e); }
    
    // Remove ActorFile
    WebFile actorFile = sceneFile.getSite().getFile("/" + name + ".java");
    if(actorFile!=null)
        try { actorFile.delete(); }
        catch(Exception e) { throw new RuntimeException(e); }

    // Wait for compile and reset scene
    StudioFilesPane.waitForCompile(sceneFile); getBrowser().reloadFile(sceneFile);
    runLater(() -> resetMainScene());
}

}