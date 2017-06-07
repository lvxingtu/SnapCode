package snap.playground;
import snap.view.View;
import snap.viewx.WebPage;

/**
 * A WebPage for Playground.
 */
public class PGPage extends WebPage {

    // The Playground
    Playground           _pg = new Playground();

/**
 * Creates a new PGPage.
 */
public PGPage()  { }

/**
 * Return the AppPane.
 */
//AppPane getAppPane()  { return getBrowser() instanceof AppBrowser? ((AppBrowser)getBrowser()).getAppPane() : null; }

/**
 * Returns the Playground.
 */
public Playground getPlayground()  { return _pg; }

/**
 * Creates UI panel.
 */
protected View createUI()  { return _pg.getUI(); }

}