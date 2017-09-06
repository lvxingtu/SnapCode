package snap.app;
import java.util.List;
import snap.view.*;
import snap.viewx.WebPage;
import snap.web.WebSite;

/**
 * Manages a list of run configurations for project.
 */
public class RunConfigsPage extends WebPage {

    // The selected RunConfig
    RunConfig   _runConfig;

/**
 * Returns the app browser.
 */
public AppBrowser getBrowser()  { return (AppBrowser)super.getBrowser(); }

/**
 * Returns the AppPane.
 */
public AppPane getAppPane()  { return getBrowser().getAppPane(); }

/**
 * Returns the Project.
 */
public WebSite getSelectedSite()  { return getAppPane().getRootSite(); }

/**
 * Returns the List of RunConfigs.
 */
public List <RunConfig> getRunConfigs()  { return RunConfigs.get(getSelectedSite()).getRunConfigs(); }

/**
 * Returns the selected run config.
 */
public RunConfig getSelectedRunConfig()
{
    if(_runConfig==null && getRunConfigs().size()>0)
        _runConfig = getRunConfigs().get(0);
    return _runConfig;
}

/**
 * Sets the selected run config.
 */
public void setSelectedRunConfig(RunConfig aConfig)  { _runConfig = aConfig; }

/**
 * Override to put in Page pane.
 */
protected View createUI()  { return new ScrollView(super.createUI()); }

/**
 * Respond to UI changes.
 */
public void respondUI(ViewEvent anEvent)
{
    // Handle AddButton
    if(anEvent.equals("AddButton")) {
        RunConfig rc = new RunConfig().setName("Untitled");
        getRunConfigs().add(rc);
        setSelectedRunConfig(rc);
    }
    
    // Handle RemoveButton
    if(anEvent.equals("RemoveButton") && getRunConfigs().size()>0) {
        getRunConfigs().remove(getSelectedRunConfig());
        setSelectedRunConfig(null);
    }
    
    // Save RunConfigs
    RunConfigs.get(getSelectedSite()).writeFile();
    getAppPane().getToolBar().setRunMenuButtonItems();
}

/**
 * Override to suppress.
 */
public void reload()  { }

}