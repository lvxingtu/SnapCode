package snap.playground;
import snap.viewx.TextPane;

/**
 * A custom class.
 */
public class PGTextPane extends TextPane {

/**
 * Creates a new PGTextPane.
 */
public PGTextPane(Playground aPG)  { }


/**
 * Initialize UI.
 */
protected void initUI()
{
    super.initUI();
    getUI().setPrefSize(900,800);
    getUI().setGrowHeight(true);
}

}