package snap.app;
import snap.javatext.JavaTextBox;
import snap.gfx.*;
import snap.project.*;
import snap.view.*;
import snap.web.WebFile;

/**
 * A pane/panel to show current build issues.
 */
public class BuildIssuesPane extends ViewOwner {

    // The AppPane
    AppPane                 _appPane;
    
    // The project
    Project                 _proj;
    
    // The selected issue
    BuildIssue              _selectedIssue;

/**
 * Creates a new ProblemsPane.
 */
public BuildIssuesPane(AppPane anAP)  { _appPane = anAP; }

/**
 * Returns the selected project.
 */
public Project getProject()  { return _proj!=null? _proj : (_proj=Project.get(_appPane.getSelectedSite())); }

/**
 * Returns the array of current build issues.
 */
public BuildIssue[] getIssues()  { return getProject().getBuildIssues().getArray(); }

/**
 * Returns the selected issue.
 */
public BuildIssue getSelectedIssue()  { return _selectedIssue; }

/**
 * Sets the selected issue.
 */
public void setSelectedIssue(BuildIssue anIssue)  { _selectedIssue = anIssue; }

/**
 * Returns the string overview of the results of last build.
 */
public String getBuildStatusText()
{
    Project proj = getProject();
    int ec = proj.getBuildIssues().getErrorCount(), wc = proj.getBuildIssues().getWarningCount();
    String error = ec==1? "error" : "errors", warning = wc==1? "warning" : "warnings";
    return String.format("%d %s, %d %s", ec, error, wc, warning);
}

/**
 * Initialize UI.
 */
protected void initUI()
{
    // Configure ErrorsList
    ListView <BuildIssue> errorsList = getView("ErrorsList", ListView.class);
    errorsList.setCellConfigure(this :: configureErrorsCell);
    errorsList.setRowHeight(24);
}

/**
 * Respond to UI controls.
 */
public void respondUI(ViewEvent anEvent)
{
    // Handle ErrorsList
    if(anEvent.equals("ErrorsList")) {
        BuildIssue issue = getSelectedIssue();
        if(issue!=null) {
            WebFile file = issue.getFile();
            String urls = file.getURL().getString() + "#LineNumber=" + issue.getLineNumber();
            _appPane.getBrowser().setURLString(urls);
        }
    }
}

/**
 * Configures an errors list cell.
 */
protected void configureErrorsCell(ListCell <BuildIssue> aCell)
{
    BuildIssue aBI = aCell.getItem(); if(aBI==null) return;
    String text = String.format("%s (%s:%d)", aBI.getText(), aBI.getFile().getName(), aBI.getLine()+1);
    text = text.replace('\n', ' ');
    aCell.setText(text); aCell.setImage(aBI.isError()? ErrorImage : WarningImage);
    aCell.getGraphic().setPadding(2,5,2,5);
}

/** Returns the error icon. */
static Image ErrorImage = Image.get(JavaTextBox.class, "ErrorMarker.png");
static Image WarningImage = Image.get(JavaTextBox.class, "WarningMarker.png");

}