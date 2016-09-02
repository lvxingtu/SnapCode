/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snap.project;
import java.io.Closeable;
import java.io.IOException;
import java.net.*;
import java.util.*;
import snap.web.*;

/**
 * A class loader for Project.
 */
public class ProjectClassLoader extends WebClassLoader implements Closeable {

    // The project
    Project       _proj;

/**
 * Creates a new AppClassLoader.
 */
public ProjectClassLoader(Project aProject)
{
    super(getURLClassLoader(aProject), aProject.getSite());  _proj = aProject;
}

/**
 * Override to catch and ignore VerifyError(s).
 */
@Override
protected Class<?> findClass(String aName) throws ClassNotFoundException
{
    try { return super.findClass(aName); }
    catch(VerifyError e) { return null; }
    catch(NoClassDefFoundError e) { return null; }
}

/**
 * Returns a build file for site.
 */
public WebFile getBuildFile(String aPath)  { return _proj.getBuildFile(aPath, false, false); }

/**
 * Closes this class loader (really it's parent).
 */
public void close() throws IOException
{
    ClassLoader p = getParent();
    if(p instanceof Closeable && p!=Project.class.getClassLoader() && p!=ClassLoader.getSystemClassLoader().getParent())
        ((Closeable)getParent()).close();
}

/**
 * Returns a ClassLoader for project jars.
 */
private static ClassLoader getURLClassLoader(Project aProj)
{
    // Get Project URLs
    List <URL> ulist = new ArrayList();
    for(String p : aProj.getClassPaths()) { URL u = WebURL.getURL(p).getURL(); ulist.add(u); }
    URL urls[] = ulist.toArray(new URL[ulist.size()]);
    
    // Create ClassLoader for URLs and return
    ClassLoader cldr = ClassLoader.getSystemClassLoader().getParent();
    if(urls.length>0) cldr = new URLClassLoader(urls, cldr);
    return cldr;
}

}