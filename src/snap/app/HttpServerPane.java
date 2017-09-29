package snap.app;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import snap.gfx.*;
import snap.util.*;
import snap.view.*;
import com.sun.net.httpserver.*;
import snap.web.*;

/**
 * HTTPServerPane provides UI for managing an HTTP-Server for this project.
 */
public class HttpServerPane extends ViewOwner {
    
    // The SitePane
    SitePane       _sitePane;
    
    // The WebSite
    WebSite        _site;

    // The HTTPServer
    HttpServer     _server;
    
    // The port
    int            _port = 8080;
    
    // Cache-control: max-age=20
    String         _cacheControl = "max-age=20";
    
    // Whether server is running
    boolean        _running;
    
    // The TextView
    TextView       _textView;
    
    // DateFormat for GMT time
    static DateFormat  _fmt;
    
    // Colors
    static Color  OK_COLOR = Color.LIGHTBLUE;
    static Color  ERR_COLOR = Color.RED;

/**
 * Creates a new HTTPServerPane for SitePane.
 */
public HttpServerPane(SitePane aSitePane)
{
    _sitePane = aSitePane;
    _site = aSitePane.getSite().getURL("/bin").getAsSite();
}

/**
 * Init UI.
 */
protected void initUI()
{
    // Get TextView and configure
    _textView = getView("LogText", TextView.class);
    _textView.setRich(true); _textView.setWrapText(true);
    
    // Make font bigger and increase space between lines
    _textView.getRichText().setDefaultStyle(_textView.getRichText().getDefaultStyle().copyFor(Font.Arial12));
    TextLineStyle lstyle = _textView.getRichText().getDefaultLineStyle();
    TextLineStyle lstyle2 = lstyle.copyFor(TextLineStyle.SPACING_KEY,2);
    _textView.getRichText().setDefaultLineStyle(lstyle2);
}

/**
 * Update the UI.
 */
protected void resetUI()
{
    setViewText("StartButton", isRunning()? "Stop Server" : "Start Server");
}

/**
 * Respond to UI.
 */
protected void respondUI(ViewEvent anEvent)
{
    // Handle StartButton
    if(anEvent.equals("StartButton")) {
        if(!isRunning()) startServer();
        else stopServer();
    }
}

/**
 * Returns the server.
 */
public HttpServer getServer()
{
    if(_server!=null) return _server;
    try { _server = createServer(); }
    catch(Exception e) { throw new RuntimeException(e); }
    return _server;
}

/**
 * Creates the server.
 */
protected HttpServer createServer() throws IOException
{
    HttpServer server = HttpServer.create(new InetSocketAddress(_port), 0);
    server.createContext("/", new SimpleHttpHandler());
    return server;
}

/**
 * Returns whether server is running.
 */
public boolean isRunning()  { return _server!=null && _running; }

/**
 * Starts the server.
 */
public void startServer()
{
    if(_running) return;
    getServer().start(); _running = true;
    getUI();
    append("Started Server\n");
}

/**
 * Stops the server.
 */
public void stopServer()
{
    if(!_running) return;
    getServer().stop(0);
    _running = false;
}

/**
 * Prints exchange to server.
 */
void printExchange(HttpExchange anExch)
{
    // Append Date
    append("["); append(new Date().toString()); append("] ");
    
    // Append method and path
    String meth = anExch.getRequestMethod();
    String path = anExch.getRequestURI().getPath();
    append("\""); append(meth,OK_COLOR); append(" ",OK_COLOR); append(path,OK_COLOR); append("\" ");
    
    // Append User-Agent
    Headers hdrs = anExch.getRequestHeaders();
    List <String> userAgents = hdrs.get("User-agent");
    if(userAgents!=null)
        append(StringUtils.getStringQuoted(ListUtils.joinStrings(userAgents,",")));

    //for(String hdr : hdrs.keySet()) append(hdr + " = " + ListUtils.joinStrings(hdrs.get(hdr),",") + '\n');
    append("\n");
}

/**
 * Appends to the text view.
 */
void append(String aStr)  { append(aStr, Color.BLACK); }

/**
 * Appends to the text view.
 */
void append(String aStr, Color aColor)
{
    // If not main thread, return on main thread
    if(!getEnv().isEventThread()) { runLater(() -> append(aStr, aColor)); return; }
    
    // Append text
    int len = _textView.length();
    TextStyle tstyle = _textView.getStyleAt(len).copyFor(aColor);
    _textView.replaceChars(aStr, tstyle, len, len, false);
}

/**
 * Returns a GMT date string.
 */
private static String getGMT(Date aDate)
{
    if(_fmt==null) {
        _fmt = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
        _fmt.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    return _fmt.format(aDate);
}

/**
 * A simple HttpHandler.
 */
private class SimpleHttpHandler implements HttpHandler {
    
    /** Handle. */
    public void handle(HttpExchange anExch) throws IOException
    {
        // Get method
        String meth = anExch.getRequestMethod();
        String path = anExch.getRequestURI().getPath();
        
        // Add ResponseHeaders: Server, Keep-alive
        Headers hdrs = anExch.getResponseHeaders();
        hdrs.add("server", "SnapCode 1.0");
        hdrs.add("Connection", "keep-alive");
        
        // Handle method
        if(meth.equals("HEAD")) handleHead(anExch);
        else if(meth.equals("GET")) handleGet(anExch);
        
        printExchange(anExch);
    }

    /** Handle HEAD. */
    public void handleHead(HttpExchange anExch) throws IOException
    {
        // Get path and File
        String path = anExch.getRequestURI().getPath();
        WebFile file = _site.getFile(path);
        
        // If file not found, return NOT_FOUND (404) and return
        if(file==null) {
            anExch.sendResponseHeaders(HTTPResponse.NOT_FOUND,-1); return; }
            
        // Get bytes
        file.reload();
        byte bytes[] = file.getBytes();
        
        // Add ResponseHeaders: last-modified, cache-control, content-length, content-type
        Headers hdrs = anExch.getResponseHeaders();
        hdrs.add("last-modified", getGMT(file.getModifiedDate()));
        hdrs.add("cache-control", _cacheControl);
        hdrs.add("content-length", String.valueOf(bytes.length));
        String mtype = MIMEType.getType(file.getType());
        if(mtype!=null) hdrs.add("content-type", mtype);
        
        // Get bytes and append
        anExch.sendResponseHeaders(HTTPResponse.OK,-1);
    }

    /** Handle GET. */
    public void handleGet(HttpExchange anExch) throws IOException
    {
        // Get path and File
        String path = anExch.getRequestURI().getPath();
        WebFile file = _site.getFile(path);
        
        // If file not found, return NOT_FOUND (404) and return
        if(file==null) {
            anExch.sendResponseHeaders(HTTPResponse.NOT_FOUND,0); return; }
        
        // Get bytes
        byte bytes[] = file.getBytes();
        
        // Add ResponseHeaders: last-modified, content-length, content-type
        Headers hdrs = anExch.getResponseHeaders();
        hdrs.add("last-modified", getGMT(file.getModifiedDate()));
        hdrs.add("cache-control", _cacheControl);
        hdrs.add("content-length", String.valueOf(bytes.length));
        String mtype = MIMEType.getType(file.getType());
        if(mtype!=null) hdrs.add("content-type", mtype);
        
        // Append bytes
        anExch.sendResponseHeaders(200,bytes.length);
        OutputStream os = anExch.getResponseBody();
        os.write(bytes);
        os.close();
    }
}

}