package snap.javaparse;
import java.lang.reflect.Field;
import snap.util.ClassUtils;

/**
 * A JNode for Enum constants.
 */
public class JEnumConst extends JMemberDecl
{
    // The arguments
    String          _args;
    
    // The class or interface body
    String          _classBody;
    
/**
 * Returns the arguments.
 */
public String getArgs()  { return _args; }

/**
 * Sets the arguments.
 */
public void setArgs(String aString)  { _args = aString; }

/**
 * Returns the class decl.
 */
public String getClassBody()  { return _classBody; }

/**
 * Sets the class decl.
 */
public void setClassBody(String aBody)  { _classBody = aBody; }

/**
 * Get class name from parent enum declaration.
 */
protected JavaDecl getDeclImpl()
{
    Class cls = getParent().getEvalClass();
    Field field = cls!=null? ClassUtils.getField(cls, getName()) : null;
    return field!=null? getJavaDecl(field) : null;
}

/**
 * Override to resolve enum id.
 */
protected JavaDecl getDeclImpl(JNode aNode)
{
    if(aNode==_id) return getDecl();
    return super.getDeclImpl(aNode);
}

}