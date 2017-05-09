package snap.javasnap;
import java.util.List;
import snap.gfx.*;
import snap.javakit.*;
import snap.view.*;

/**
 * A JNodeView subclass for JClassDecl.
 */
public class JClassDeclView <JNODE extends JClassDecl> extends JNodeView <JNODE> {

/**
 * Creates a new JClassDeclView for given JClassDecl.
 */
public JClassDeclView(JNODE aCD)  { super(aCD); }

/**
 * Override.
 */
protected void updateUI()
{
    super.updateUI(); setType(Type.None);
}

/**
 * Updates UI for HBox.
 */
protected void updateHBox(HBox spane)
{
    //JClassDecl cd = getJNode();
    //Label label = createLabel(cd.getName()); label.setFont(label.getFont().deriveFont(18));
    //spane.addChild(label);
    spane.setSpacing(12);
    
    JClassDecl cd = getJNode();
    JExprId id = cd.getId();
    List <JType> exts = cd.getExtendsTypes();
    JType ext = exts.size()>0? exts.get(0) : null;
    
    // Add JNodeView for id
    JNodeView idView = new ClassDeclIdView(id);
    spane.addChild(idView);
    
    // Add JNodeView for extnds type
    if(ext!=null) {
        
        // Add separator label
        Label label = new Label(" extends "); label.setFont(Font.Arial14.deriveFont(16));
        label.setTextFill(Color.WHITE);
        spane.addChild(label);
        
        // Add TypeView
        JNodeView typView = new ClassDeclTypeView(ext);
        spane.addChild(typView);
    }
}

/**
 * Returns a string describing the part.
 */
public String getPartString()  { return "Class Declaration"; }

/**
 * A JNodeView subclass for JClassDecl id.
 */
public static class ClassDeclIdView <JNODE extends JExprId> extends JNodeView <JNODE> {

    /** Creates a new JTypeView for given JType. */
    public ClassDeclIdView(JNODE aCD)  { super(aCD); }

    /** Override. */
    protected void updateUI()
    {
        super.updateUI(); setType(Type.Plain); setSeg(Seg.Middle); setColor(ClassDeclColor);
        getHBox().setMinSize(240,35);
        _bg.setBorder(ClassDeclColor.darker(),2);
    }
    
    /** Updates UI for HBox. */
    protected void updateHBox(HBox spane)
    {
        JExprId id = getJNode();
        Label label = createLabel(id.getName()); label.setFont(label.getFont().deriveFont(20));
        spane.addChild(label);
    }
    
    /** Returns a string describing the part. */
    public String getPartString()  { return "ClassId"; }
}

/**
 * A JNodeView subclass for JClassDecl extends type.
 */
public static class ClassDeclTypeView <JNODE extends JType> extends JNodeView <JNODE> {

    /** Creates a new JTypeView for given JType. */
    public ClassDeclTypeView(JNODE aCD)  { super(aCD); }

    /** Override. */
    protected void updateUI()
    {
        super.updateUI(); setType(Type.Plain); setSeg(Seg.Middle); setColor(ClassDeclColor);
        getHBox().setMinSize(120,25);
    }
    
    /** Updates UI for HBox. */
    protected void updateHBox(HBox spane)
    {
        JType typ = getJNode();
        Label label = createLabel(typ.getName()); label.setFont(label.getFont().deriveFont(14));
        spane.addChild(label);
    }
    
    /** Returns a string describing the part. */
    public String getPartString()  { return "Type"; }
}

}