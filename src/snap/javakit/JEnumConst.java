package snap.javakit;

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
    String name = getName();
    JClassDecl cls = (JClassDecl)getParent();
    JavaDecl cdecl = cls.getDecl();
    JavaDecl edecl = cdecl.getHpr().getField(name);
    return edecl;
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