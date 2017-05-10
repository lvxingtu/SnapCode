package snap.javakit;
import java.util.Collections;
import java.util.List;

/**
 * A JNode for Enum constants.
 */
public class JEnumConst extends JMemberDecl
{
    // The args
    List <JExpr>   _args = Collections.EMPTY_LIST;

    // The class or interface body
    String          _classBody;
    
/**
 * Returns the arguments.
 */
public List <JExpr> getArgs()  { return _args; }

/**
 * Sets the arguments.
 */
public void setArgs(List <JExpr> theArgs)
{
    if(_args!=null) for(JExpr arg : _args) removeChild(arg);
    _args = theArgs;
    if(_args!=null) for(JExpr arg : _args) addChild(arg, -1);
}

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