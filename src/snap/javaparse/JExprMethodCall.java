package snap.javaparse;
import java.lang.reflect.*;
import java.util.List;
import snap.util.ClassUtils;

/**
 * This class represents a method call in code.
 */
public class JExprMethodCall extends JExpr {

    // The identifier
    JExprId        _id;
    
    // The args
    List <JExpr>   _args;

/**
 * Creates a new method call.
 */
public JExprMethodCall()  { }

/**
 * Creates a new method call for given identifier (method name) and arg list.
 */
public JExprMethodCall(JExprId anId, List theArgs)  { setId(anId); setArgs(theArgs); }

/**
 * Returns the identifier.
 */
public JExprId getId()  { return _id; }

/**
 * Sets the identifier.
 */
public void setId(JExprId anId)
{
    if(_id==null) addChild(_id=anId, 0);
    else replaceChild(_id, _id=anId);
    if(_id!=null) setName(_id.getName());
}

/**
 * Returns the number of arguments.
 */
public int getArgCount()  { return _args.size(); }

/**
 * Returns the individual argument at index.
 */
public JExpr getArg(int anIndex)  { return _args.get(anIndex); }

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
        classes[i] = arg!=null? arg.getEvalClass() : null; }
    return classes;
}

/**
 * Returns the arg eval types.
 */
public JavaDecl[] getArgEvalTypes()
{
    List <JExpr> args = getArgs();
    JavaDecl etypes[] = new JavaDecl[args.size()];
    for(int i=0, iMax=args.size(); i<iMax; i++) { JExpr arg = args.get(i);
        etypes[i] = arg!=null? arg.getEvalType() : null; }
    return etypes;
}

/**
 * Returns the method from parent class and arg types.
 */
public Method getMethod()
{
    // Get method name, args and scope class for expression
    String name = getName();
    Class argClasses[] = getArgClasses();
    Class sclass = getScopeNodeEvalClass();
    
    // Iterate up enclosing classes until we find method
    for(Class c=sclass; c!=null; c=c.getEnclosingClass()) {
        Method meth = null; try { meth = ClassUtils.getMethod(c, name, argClasses); }
        catch(Throwable t) { }  // Since the compiled app can be in any weird state
        if(meth!=null)
            return meth;
    }
    
    // If scope class is interface, try evaluating on Object
    if(sclass!=null && sclass.isInterface()) {
        try { return ClassUtils.getMethod(Object.class, name, argClasses); }
        catch(Throwable t) { }  // Since the compiled app can be in any weird state
    }
    
    // See if method is from static import
    Member mem = getFile().getImportClassMember(name, argClasses);
    if(mem instanceof Method)
        return (Method)mem;
        
    // Return null since not found
    return null;
}

/**
 * Tries to resolve the method declaration for this node.
 */
protected JavaDecl getDeclImpl()
{
    // Get method using node parent class and args (just return if not found)
    Method meth = getMethod();
    if(meth==null)
        return null;
    
    // Get decl for method
    JavaDecl decl = getJavaDecl(meth);
    return decl;
}

/**
 * Override to handle method name.
 */
protected JavaDecl getDeclImpl(JNode aNode)
{
    if(aNode==_id) return getDecl();
    return super.getDeclImpl(aNode);
}
        
/**
 * Override to resolve Decl.EvalType from ParentExpr.EvalType.
 */
protected JavaDecl getEvalTypeImpl(JNode aNode)
{
    // Handle MethodCall id
    if(aNode==_id) return getEvalType();

    // Handle this node
    else if(aNode==this) {
        JavaDecl mdecl = aNode.getDecl(); if(mdecl==null) return null;
        JavaDecl etype = mdecl.getEvalType();
        JavaDecl scopeType = getScopeNodeEvalType();
        
        // If eval type is TypeVar, try to resolve
        if(etype.isTypeVar()) { String name = etype.getName();
            
            // See if TypeVar can be resolved by method
            JavaDecl resolvedDecl = getResolvedTypeVarForMethod(name, mdecl);
            if(resolvedDecl!=null)
                return resolvedDecl;
            
            // See if TypeVar can be resolved by ScopeNode.Type
            if(scopeType.isParamType()) {
                resolvedDecl = scopeType.getTypeVar(name);
                if(resolvedDecl!=null)
                    return resolvedDecl;
            }
            
            // Otherwise, just return TypeVar default
            return etype.getEvalType();
        }
    }
    
    // Do normal version
    return super.getEvalTypeImpl(aNode);
}

/**
 * Resolves a TypeVar for given method decl and arg types.
 */
public JavaDecl getResolvedTypeVarForMethod(String aName, JavaDecl aMethDecl)
{
    // If no type var for given name, just return
    if(aMethDecl.getTypeVar(aName)==null)
        return null;
        
    // Iterate over method arg types to see if any can resolve the type var
    JavaDecl argTypes[] = aMethDecl.getArgTypes();
    for(int i=0,iMax=argTypes.length;i<iMax;i++) { JavaDecl arg = argTypes[i];
    
        // If method arg is TypeVar with same name, return arg expr eval type (if not null)
        if(arg.isTypeVar() && arg.getName().equals(aName)) {
            JExpr argExpr = getArg(i); if(argExpr==null) continue;
            JavaDecl argEvalType = argExpr.getEvalType();
            return argEvalType;
        }
        
        // If method arg is ParamType with matching param TypeVar,
        if(arg.isParamType()) {
            
            // Iterate over ParamType params
            for(JavaDecl parg : arg.getArgTypes()) {
                
                // If TypeVar with matching name, see if arg eval type can resolve
                if(parg.isTypeVar() && parg.getName().equals(aName)) {
                    
                    // Get arg expr and eval type
                    JExpr argExpr = getArg(i); if(argExpr==null) continue;
                    JavaDecl argEvalType = argExpr.getEvalType(); if(argEvalType==null) continue;
                    if(argEvalType.isParamType())
                        return argEvalType.getArgTypes()[0];
                }
            }
        }
    }
    
    // Return null since TypeVar name couldn't be resolved by method args
    return null;
}

/**
 * Returns the part name.
 */
public String getNodeString()  { return "MethodCall"; }

}