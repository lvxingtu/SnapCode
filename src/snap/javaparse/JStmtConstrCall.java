package snap.javaparse;
import java.lang.reflect.Constructor;
import java.util.*;

/**
 * A JStmt subclass to represent an explicit constructor invocation, like: this(x) or super(y).
 * Found in first line of JContrDecl only.
 */
public class JStmtConstrCall extends JStmt {

    // The identifier
    List <JExprId>        _idList = new ArrayList();
    
    // The args
    List <JExpr>   _args;

/**
 * Returns the list of ids.
 */
public List <JExprId> getIds()  { return _idList; }

/**
 * Adds an Id.
 */
public void addId(JExprId anId)  { _idList.add(anId); addChild(anId); }

/**
 * Returns the method arguments.
 */
public List <JExpr> getArgs()  { return _args; }

/**
 * Sets the method arguments.
 */
public void setArgs(List <JExpr> theArgs)
{
    if(_args!=null) for(JExpr arg : _args) removeChild(arg);
    _args = theArgs;
    if(_args!=null) for(JExpr arg : _args) addChild(arg, -1);
}

/**
 * Returns the arg classes.
 */
public Class[] getArgClasses()
{
    List <JExpr> args = getArgs();
    Class classes[] = new Class[args.size()];
    for(int i=0, iMax=args.size(); i<iMax; i++) { JExpr arg = args.get(i);
        classes[i] = arg!=null? arg.getJClass() : null; }
    return classes;
}

/**
 * Override to get declaration from actual Constructor.
 */
@Override
protected JavaDecl getDeclImpl()
{
    Constructor c = getConstructor();
    return c!=null? new JavaDecl(c) : null;
}

/**
 * Returns the java.lang.reflect Constructor for this method decl from the compiled class.
 */
public Constructor getConstructor()
{
    JClassDecl cd = getEnclosingClassDecl();
    Class cls = cd!=null? cd.getJClass() : null; if(cls==null) return null;
    try { return cls.getSuperclass().getDeclaredConstructor(getArgClasses()); }
    catch(Throwable e) { return null; }
}

}