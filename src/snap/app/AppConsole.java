package snap.app;
import java.util.*;
import snap.data.*;
import snap.util.StringUtils;
import snap.view.*;
import snap.viewx.TextConsole;
import snap.web.*;

/**
 * A pane to show a console for app.
 */
public class AppConsole extends ViewOwner {

    // The AppPane
    AppPane            _appPane;

    // The ConsoleText
    ConsoleText        _consoleText;

/**
 * Returns the AppPane.
 */
public AppPane getAppPane()  { return _appPane; }

/**
 * Returns the site.
 */
public WebSite getSite()  { return getAppPane().getSelectedSite(); }

/**
 * Returns the data site.
 */
public DataSite getDataSite()  { return DataSite.get(getAppPane().getSelectedSite()); }

/**
 * Creates the UI.
 */
protected View createUI()
{
    _consoleText = new ConsoleText(); _consoleText.setPrompt("prompt> ");
    ScrollView spane = new ScrollView(); spane.setContent(_consoleText);
    return spane;
}

/**
 * A text area for console processing.
 */
public class ConsoleText extends TextConsole {

    /** Executes command. */
    protected String executeCommandImpl(String aCommand)
    {
        // Remove semi-colon
        aCommand = StringUtils.delete(aCommand, ";");
        
        // Handle show tables
        if(aCommand.equalsIgnoreCase("show tables")) {
            StringBuffer sb = new StringBuffer();
            for(Entity entity : getDataSite().getSchema().getEntities())
                sb.append(entity.getName()).append("\n");
            return sb.toString();
        }
        
        // Handle get command
        if(aCommand.startsWith("get "))
            return executeGet(aCommand.substring(4));
        
        // Handle select command
        if(aCommand.startsWith("select "))
            return executeSelect(aCommand.substring(7));
        
        // Otherwise, do default version
        return super.executeCommandImpl(aCommand);
    }
    
    /** Execute a help command. */
    public String executeHelp(String aCommand)
    {
        StringBuffer sb = new StringBuffer();
        sb.append("show tables\n");
        sb.append("get [ file name ]\n");
        sb.append("select [ property, ... ] from [ table_name ]\n");
        sb.append("print [ expression ]\n");
        sb.append("clear");
        return sb.toString();
    }
    
    /** Execute get command. */
    public String executeGet(String aCommand)
    {
        WebFile file = getSite().getFile("/" + aCommand);
        if(file!=null)
            return StringUtils.getString(file.getBytes());
        return "File not found";
    }
    
    /** Execute select command. */
    public String executeSelect(String aCommand)
    {
        // Get from index
        int from = StringUtils.indexOfIC(aCommand, "from");
        if(from<0)
            return "Syntax error";
        
        // Get entity
        String entityName = aCommand.substring(from + 4).trim();
        Entity entity = getDataSite().getSchema().getEntity(entityName);
        if(entity==null)
            return "Table not found";
        
        // Get properties
        List <Property> properties = new ArrayList();
        String props[] = aCommand.substring(0, from).split(",");
        for(String prop : props) {
            if(prop.trim().equals("*")) {
                properties.addAll(entity.getProperties());
                break;
            }
            Property property = entity.getProperty(prop.trim());
            if(property!=null)
                properties.add(property);
        }
        
        // Create string buffer
        StringBuffer sb = new StringBuffer();
        
        // Append headers
        for(Property prop : properties)
            sb.append(prop.getName()).append("\t");
        if(properties.size()>0) sb.delete(sb.length()-1, sb.length());
        sb.append("\n");
    
        // Get rows and append values
        List <Row> rows = getDataSite().getRows(new Query(entity));
        for(Row row : rows) {
            for(Property prop : properties)
                sb.append(row.get(prop.getName())).append("\t");
            if(properties.size()>0) sb.delete(sb.length()-1, sb.length());
            sb.append("\n");
        }
        
        // Return string
        return sb.toString();
    }
}

}