package snap.javaparse;
import java.io.*;
import java.util.List;
import snap.util.SnapUtils;
import snap.view.*;
import snap.viewx.TextPane;

/**
 * A class to view Java Class files.
 */
public class ClassViewer extends TextPane {
    
/**
 * InitUI.
 */
protected void initUI()
{
    super.initUI();
    enableEvents(getTextView(), DragEvents);
    getUI().setPrefSize(800,1000);
}

/**
 * RespondUI.
 */
protected void respondUI(ViewEvent anEvent)
{
    // Do normal version
    super.respondUI(anEvent);
    
    // Handle DragEvents
    if(anEvent.isDragEvent()) {
        if(anEvent.hasDragFiles()) anEvent.acceptDrag();
        if(!anEvent.isDragDrop()) return;
        List <File> files = anEvent.getDragFiles();
        loadClassFile(files.get(0));
    }
}

/**
 * Loads the text from source.
 */
public void loadClassFile(Object aSource)
{
    ClassFileData cfd = new ClassFileData();
    InputStream istream = SnapUtils.getInputStream(aSource);
    try { cfd.read(new DataInputStream(istream)); }
    catch(Exception e) { System.err.println(e); return; }
    StringBuffer sb = new StringBuffer();
    sb.append("Class: ").append(cfd.classNames.cpThisClass).append('\n');
    sb.append("Superclass: ").append(cfd.classNames.cpSuperClass).append('\n');
    sb.append("Magic Number: ").append(cfd.magicNumber).append('\n');
    sb.append("Major Version: ").append(cfd.majorVersion).append('\n');
    sb.append("Minor Version: ").append(cfd.minorVersion).append('\n').append('\n');
    
    sb.append("Constant Pool:\n");
    ClassFileData.Constant c;
    for(ClassFileData.Constant con : (List <ClassFileData.Constant>)cfd.constantPool._constants)
        sb.append(con).append('\n');
        
    getTextView().setText(sb.toString());
}

/**
 * Main method.
 */
public static void main(String args[])
{
    ClassViewer cv = new ClassViewer(); cv.setWindowVisible(true);
}

}