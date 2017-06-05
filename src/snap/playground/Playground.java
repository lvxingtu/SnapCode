package snap.playground;
import snap.view.*;

/**
 * A pane to do Java REPL (Read, Eval, Print, Loop).
 */
public class Playground extends ViewOwner {
    
    // The TextPane
    PGTextPane        _textPane = new PGTextPane(this);
    
    // The TabPane
    PGTabPane         _tabPane = new PGTabPane(this);

/**
 * Creates a new Playground.
 */
public Playground()  { }

/**
 * Creates the UI.
 */
protected View createUI()
{
    SplitView split = new SplitView(); split.setPrefSize(800,1000); split.setVertical(true);
    split.setItems(_textPane.getUI(), _tabPane.getUI());
    return split;
}

/**
 * Initialize UI.
 */
protected void initUI()
{
    super.initUI();
}

/**
 * Standard main method.
 */
public static void main(String args[])
{
    Playground pg = new Playground(); pg.getWindow().setTitle("Java Playground");
    pg.setWindowVisible(true);
}

}