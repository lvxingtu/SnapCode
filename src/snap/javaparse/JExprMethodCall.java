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
}

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
 * Returns the method by querying parent class ref.
 */
public Method getMethod()
{
    Class pclass = getParentClass(); if(pclass==null) return null;
    for(Class c=pclass; c!=null; c=c.getEnclosingClass()) {
        Method method = null;
        try { method = ClassUtils.getMethod(c, getName(), getArgClasses()); }
        catch(Throwable t) { }  // Since the compiled app can be in any weird state
        if(method!=null)
            return method; }
    return null;
}

/**
 * Resolves the name from Identifier or Method.
 */
protected String getNameImpl()  { return _id!=null? _id.getName() : null; }

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
    JavaDecl decl = new JavaDecl(meth);
    
    // If method return type is TypeVariable name, try to resolve it to real class
    Type rtype = meth.getGenericReturnType();
    if(rtype instanceof TypeVariable) { TypeVariable tv = (TypeVariable)rtype;

        // See if method can resolve TypeVariable from method args
        String name = tv.getName();
        Class rclass = resolveTypeVarFromArgs(meth, name);
        if(rclass!=null) {
            decl._tname = rclass.getName(); return decl; }
            
        // See if parent class heirarchy can resolve TypeVariable name
        Class pclass = getParentClass();
        rclass = resolveTypeName(pclass, name);
        if(rclass!=null) {
            decl._tname = rclass.getName(); return decl; }
            
        // This should never happen - TypeVar should be defined either in method or class
        System.err.println("JExprMethodCall.getDeclImpl: Can't type name for method: " + meth);
    }
    
    // Return decl
    return decl;
}

/** Tries to resolve a Type name to a class from a method's TypeVariables and args. */
private Class resolveTypeVarFromArgs(Method aMeth, String aName)
{
    // See if TypeVariable exists for name (just return if null)
    TypeVariable tv = null;
    for(TypeVariable t : aMeth.getTypeParameters()) if(t.getName().equals(aName)) { tv = t; break; }
    if(tv==null) return null;
    
    // Iterate over parameters and see if matching TypeVariable arg can resolve
    Type ptypes[] = aMeth.getGenericParameterTypes();
    for(int i=0, iMax=ptypes.length; i<iMax; i++) { Type t = ptypes[i];
    
        // If TypeVariable with same name, return class for arg expression
        if(t instanceof TypeVariable && ((TypeVariable)t).getName().equals(aName)) {
            JExpr expr = getArgs().get(i);
            Class cls = expr.getJClass();
            if(cls!=Object.class)
                return cls;
        }
        
        // If ParameterizedType with matching type args...
        else if(t instanceof ParameterizedType) { ParameterizedType pt = (ParameterizedType)t;
            Type targs[] = pt.getActualTypeArguments();
            for(Type t2 : targs)
                if(t2 instanceof TypeVariable && ((TypeVariable)t2).getName().equals(aName)) {
                    JExpr expr = getArgs().get(i);
                    Class cls = expr.getJClass();
                    if(cls==Class.class && expr instanceof JExprChain) { JExprChain ec = (JExprChain)expr;
                        cls = ec.getExpr(ec.getExprCount()-2).getJClass(); }
                    //if(cls!=Object.class)
                        return cls;
                }
        }
    }
    
    // Return type from since type name not found or not resolved
    return getTypeClass(tv);
}

/** Returns the class name, converting primitive arrays to 'int[]' instead of '[I'. */
private Class resolveTypeName(Type aType, String aName)
{
    // Handle Class
    if(aType instanceof Class) { Class cls = (Class)aType;
        for(Type t : cls.getTypeParameters()) {
            Class c = resolveTypeName(t, aName); if(c!=null)
                return c; }
        Class scls = cls.getSuperclass(); Type stype = cls.getGenericSuperclass();
        if(scls!=Object.class && scls!=null)
            return resolveTypeName(stype, aName);
    }
    
    // Handle GenericArrayType
    if(aType instanceof GenericArrayType) { GenericArrayType gat = (GenericArrayType)aType;
        return resolveTypeName(gat.getGenericComponentType(), aName); }
        
    // Handle ParamterizedType
    if(aType instanceof ParameterizedType) { ParameterizedType pt = (ParameterizedType)aType;
        if(pt.getActualTypeArguments().length>0)
            return getTypeClass(pt.getActualTypeArguments()[0]); }
            
    // Handle TypeVariable
    if(aType instanceof TypeVariable) { TypeVariable tv = (TypeVariable)aType;
        if(tv.getName().equals(aName))
            return getTypeClass(tv); }

    // Return null since not found
    return null;
}

/** Returns the class name, converting primitive arrays to 'int[]' instead of '[I'. */
private Class getTypeClass(Type aType)
{
    Class tc = null;
    if(aType instanceof Class)
        tc = (Class)aType;
    else if(aType instanceof GenericArrayType) { GenericArrayType gat = (GenericArrayType)aType;
        tc = getTypeClass(gat.getGenericComponentType()); }
    else if(aType instanceof ParameterizedType)
        tc = getTypeClass(((ParameterizedType)aType).getRawType());
    else if(aType instanceof TypeVariable)
        tc = getTypeClass(((TypeVariable)aType).getBounds()[0]);
    else if(aType instanceof WildcardType) { WildcardType wc = (WildcardType)aType;
        if(wc.getLowerBounds().length>0) return getTypeClass(wc.getLowerBounds()[0]);
        tc = getTypeClass(wc.getUpperBounds()[0]); }
    if(tc==null)
        System.err.println("JExprMethodCall.getTypeClass: Can't get class from type: " + aType);
    return tc;
}

/**
 * Override to resolve method name.
 */
@Override
protected JavaDecl resolveName(JNode aNode)  { return aNode==_id? getDecl() : super.resolveName(aNode); }
        
/**
 * Returns the part name.
 */
public String getNodeString()  { return "MethodCall"; }

}