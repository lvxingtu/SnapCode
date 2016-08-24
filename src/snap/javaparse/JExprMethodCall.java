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
    if(meth.getGenericReturnType() instanceof TypeVariable) {
        
        // Get TypeVariable and name
        TypeVariable tv = (TypeVariable)meth.getGenericReturnType();
        String name = tv.getName();
        
        // See if method can resolve TypeVariable name
        Class rclass = resolveTypeName(meth, name);
        if(rclass!=null) {
            decl._tname = rclass.getName(); return decl; }
            
        // See if parent class heirarchy can resolve TypeVariable name
        Class pclass = getParentClass();
        rclass = resolveTypeName(pclass, name);
        if(rclass!=null) {
            decl._tname = rclass.getName(); return decl; }
        System.err.println("JExprMethodCall.getDeclImpl: Can't type name for method: " + meth); 
    }
    
    // Return decl
    return decl;
}

/** Tries to resolve a Type name to a class from a method's TypeVariables. */
private Class resolveTypeName(Method aMeth, String aName)
{
    // See if TypeVariable exists for name (just return if null)
    TypeVariable tv = null; for(TypeVariable t : aMeth.getTypeParameters()) if(t.getName().equals(aName)) tv = t;
    if(tv==null) return null;
    
    // Resolve type for args with matching type name
    for(int i=0, iMax=aMeth.getParameterCount(); i<iMax; i++) {
        Type t = aMeth.getGenericParameterTypes()[i];
        if(t instanceof ParameterizedType) { ParameterizedType pt = (ParameterizedType)t;
            for(Type t2 : pt.getActualTypeArguments())
                if(t2 instanceof TypeVariable && ((TypeVariable)t2).getName().equals(aName)) {
                    JExpr expr = getArgs().get(i);
                    Class cls = expr.getJClass();
                    if(cls==Class.class && expr instanceof JExprChain) { JExprChain ec = (JExprChain)expr;
                        cls = ec.getExpr(ec.getExprCount()-2).getJClass(); }
                    return cls;
                }
        }
    }
    
    // Return null since type name not found or not resolved
    return null;
}

/** Returns the class name, converting primitive arrays to 'int[]' instead of '[I'. */
private Class resolveTypeName(Type aType, String aName)
{
    if(aType instanceof Class) { Class cls = (Class)aType;
        for(Type t : cls.getTypeParameters()) {
            Class c = resolveTypeName(t, aName); if(c!=null)
                return c; }
        Class scls = cls.getSuperclass(); Type stype = cls.getGenericSuperclass();
        if(scls!=Object.class && scls!=null)
            return resolveTypeName(stype, aName);
    }
    if(aType instanceof GenericArrayType) { GenericArrayType gat = (GenericArrayType)aType;
        return resolveTypeName(gat.getGenericComponentType(), aName); }
    if(aType instanceof ParameterizedType) { ParameterizedType pt = (ParameterizedType)aType;
        if(pt.getActualTypeArguments().length>0)
            return getTypeClass(pt.getActualTypeArguments()[0]); }
    if(aType instanceof TypeVariable) { TypeVariable tv = (TypeVariable)aType;
        if(tv.getName().equals(aName))
            return getTypeClass(tv); }
    return null;
}

/** Returns the class name, converting primitive arrays to 'int[]' instead of '[I'. */
private Class getTypeClass(Type aType)
{
    if(aType instanceof Class)
        return (Class)aType;
    if(aType instanceof GenericArrayType) { GenericArrayType gat = (GenericArrayType)aType;
        return getTypeClass(gat.getGenericComponentType()); }
    if(aType instanceof ParameterizedType)
        return getTypeClass(((ParameterizedType)aType).getRawType());
    if(aType instanceof TypeVariable)
        return getTypeClass(((TypeVariable)aType).getBounds()[0]);
    if(aType instanceof WildcardType) { WildcardType wc = (WildcardType)aType;
        if(wc.getLowerBounds().length>0) return getTypeClass(wc.getLowerBounds()[0]);
        return getTypeClass(wc.getUpperBounds()[0]); }
    System.err.println("JExprMethodCall.getTypeClass: Can't get class from type: " + aType); return null;
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