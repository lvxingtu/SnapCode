package snap.javasnap;
import snap.javaparse.*;
import snap.view.*;

/**
 * A JNodeView subclass for JClassDecl.
 */
public class JTypeView <JNODE extends JType> extends JNodeView <JNODE> {

/**
 * Creates a new JTypeView.
 */
public JTypeView()  { }

/**
 * Creates a new JTypeView for given JType.
 */
public JTypeView(JNODE aCD)  { super(aCD); }

/**
 * Override.
 */
protected void updateUI()
{
    super.updateUI(); setType(Type.Piece); setSeg(Seg.Middle);
    setColor(PieceColor);
}

/**
 * Updates UI for HBox.
 */
protected void updateHBox(HBox spane)
{
    JType typ = getJNode();
    Label label = createLabel(typ.getName()); label.setFont(label.getFont().deriveFont(14));
    spane.addChild(label);
}

/**
 * Returns a string describing the part.
 */
public String getPartString()  { return "Type"; }

}