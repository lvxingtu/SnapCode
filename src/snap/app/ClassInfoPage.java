package snap.app;
import java.util.Arrays;
import java.util.Set;
import javakit.parse.JavaData;
import javakit.parse.JavaDecl;
import snap.viewx.TextPage;
import snap.web.*;

/**
 * A WebPage subclass to show class info for a .class file.
 */
public class ClassInfoPage extends TextPage {

/**
 * Override to return class info text instead of class file contents.
 */
protected String getDefaultText()
{
    WebFile cfile = getFile();
    String jpath = cfile.getPath().replace(".class", ".java").replace("/bin/", "/src/");
    WebFile jfile = cfile.getSite().getFile(jpath);
    JavaData jdata = jfile!=null? JavaData.get(jfile) : null; if(jdata==null) return "Class File not found";
    Set <JavaDecl> decls = jdata.getDecls(), refs = jdata.getRefs();
    
    // Create StringBuffer and append Declarations
    StringBuffer sb = new StringBuffer();
    sb.append("\n    - - - - - - - - - - Declarations - - - - - - - - - -\n\n");
    JavaDecl declArray[] = decls.toArray(new JavaDecl[0]); Arrays.sort(declArray);
    for(JavaDecl d : declArray) {
        if(d.isClass()) {
            sb.append("Class ").append(d.getFullName()).append('\n');
            for(JavaDecl d2 : declArray)
                if(!d2.isClass() && d2.getClassName().equals(d.getClassName()))
                    sb.append("    ").append(d2.getType()).append(' ').append(d2.getFullName()).append('\n');
            sb.append('\n');
        }
    }
    
    // Append References
    sb.append("\n    - - - - - - - - - - References - - - - - - - - - -\n\n");
    JavaDecl refArray[] = refs.toArray(new JavaDecl[0]); Arrays.sort(refArray);
    for(JavaDecl d : refArray) sb.append(d.getType()).append(' ').append(d.getFullName()).append('\n');
    
    // Set Text
    return sb.toString();
}

}