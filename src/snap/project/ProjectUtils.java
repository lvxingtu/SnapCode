package snap.project;
import snap.util.FileUtils;
import snap.web.WebURL;

/**
 * Utilities for Project package.
 */
public class ProjectUtils {

/**
 * Returns the Snap jar path.
 */
public static String getSnapJarPath()
{
    String path = WebURL.getURL(WebURL.class).getSiteURL().getPath();
    if(path.endsWith(".pack.gz")) // Should never happen unless in JavaWebStart
        path = FileUtils.unpack(new java.io.File(path), FileUtils.getTempDir()).getAbsolutePath();
    return path;
}

}