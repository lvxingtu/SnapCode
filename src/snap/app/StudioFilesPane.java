package snap.app;
import snap.project.*;
import snap.web.*;

/**
 * A custom AppFilesPane for Studio projects.
 */
public class StudioFilesPane  {

/**
 * Makes a project a studio project.
 */
public static WebFile makeStudioProject(WebSite aSite)
{
    WebFile studioFile = aSite.getFile("/Studio.proj");
    if(studioFile==null) {
        studioFile = createStudioFile(aSite);
        WebFile sceneFile = aSite.getFile("/Scene1.java");
        if(sceneFile==null)
            createSceneFile(aSite);
    }
    return studioFile;
}

/**
 * Creates the Studio.proj file.
 */
protected static WebFile createStudioFile(WebSite aSite)
{
    // Create content
    StringBuffer sb = new StringBuffer();

    // Create file, set content and save
    WebFile jfile = aSite.createFile("/Studio.proj", false);
    jfile.setText(sb.toString());
    try { jfile.save(); }
    catch(Exception e) { throw new RuntimeException(e); }
    return jfile;
}

/**
 * Creates the Scene file.
 */
protected static WebFile createSceneFile(WebSite aSite)
{
    // Create content
    StringBuffer sb = new StringBuffer();
    sb.append("import snap.viewx.*;\n\n");
    sb.append("/**\n").append(" * A SnapStudios Scene class.\n").append(" */\n");
    sb.append("public class Scene1 extends SnapScene {\n\n");
    sb.append("public Scene1()\n").append("{\n}\n\n");
    sb.append("public void main()\n").append("{\n}\n\n");
    sb.append("public void act()\n").append("{\n}\n\n");
    sb.append("}");

    // Create file, set content and save
    WebFile jfile = aSite.createFile("/Scene1.java", false);
    jfile.setText(sb.toString());
    try { jfile.save(); }
    catch(Exception e) { throw new RuntimeException(e); }
    //waitForCompile(jfile);
    return jfile;
}

/**
 * Waits for compile on a JavaFile (max 2 seconds).
 */
public static void waitForCompile(WebFile aFile)
{
    Project proj = Project.get(aFile);
    WebFile cfile = proj.getClassFile(aFile);
    long jmod = aFile.getLastModifiedTime();
    for(int i=0;i<60;i++) {
        if(cfile.getLastModifiedTime()>=jmod) return;
        try { Thread.sleep(50); } catch(Exception e) { }
    }
    System.err.println("StudioFilesPane: Couldn't wait for compile");
}

}