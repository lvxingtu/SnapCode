package snap.javaparse;

/**
 * Utility methods for JavaParse package.
 */
public class JavaParseUtils {

/**
 * Sets a decl in a JNode.
 */
public static void setDecl(JNode aNode, JavaDecl aDecl)  { aNode._decl = aDecl; }

/**
 * Returns whether a JavaDecl is expected.
 */
public static boolean isDeclExpected(JNode aNode)
{
    if(aNode instanceof JExprLiteral) return !((JExprLiteral)aNode).isNull();
    try { return aNode.getClass().getDeclaredMethod("getDeclImpl")!=null; }
    catch(Exception e) { return false; }
}

}