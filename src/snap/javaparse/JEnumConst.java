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
 * Resolves name from name identifier, if available.
 */
protected String getNameImpl()  { return _id!=null? _id.getName() : null; }

/**
 * Get class name from parent enum declaration.
 */
@Override
protected JavaDecl getDeclImpl()
{
    Class cls = getParent().getJClass();
    Field field = cls!=null? ClassUtils.getField(cls, getName()) : null;
    return field!=null? getJavaDecl(field) : null;
}

/**
 * Override to resolve enum id.
 */
@Override
protected JavaDecl resolveName(JNode aNode)  { return aNode==_id? getDecl() : super.resolveName(aNode); }

}