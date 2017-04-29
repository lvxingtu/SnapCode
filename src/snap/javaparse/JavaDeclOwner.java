package snap.javaparse;
import java.lang.reflect.*;
import java.util.*;
import snap.util.ClassUtils;

/**
 * A class that manages all the JavaDecls for a project.
 */
public abstract class JavaDeclOwner {

    // A map of class/package names to JavaDecls to provide JavaDecls for project
    Map <String,JavaDecl>   _decls = new HashMap();
    
/**
 * Returns a JavaDecl for object.
 */
public JavaDecl getJavaDecl(Object anObj)
{
    // Handle String (class or package name)
    if(anObj instanceof String) { String name = (String)anObj;
    
        // If decl exists for name, just return
        JavaDecl jd = _decls.get(name); if(jd!=null) return jd;
        
        // If class exists, forward to getClassDecl()
        Class cls = getClass(name);
        if(cls!=null)
            return getClassDecl(cls);
        return null;
    }
    
    // Handle Class
    JavaDecl jd = null;
    if(anObj instanceof Class) { Class cls = (Class)anObj;
        jd = getClassDecl(cls); }
    
    // Handle Field
    else if(anObj instanceof Field) { Field field = (Field)anObj; Class cls = field.getDeclaringClass();
        JavaDecl decl = getClassDecl(cls);
        JavaDeclHpr declHpr = decl.getHpr();
        jd = declHpr.getFieldDecl(field);
    }
    
    // Handle Method
    else if(anObj instanceof Method) { Method meth = (Method)anObj; Class cls = meth.getDeclaringClass();
        JavaDecl decl = getClassDecl(cls);
        JavaDeclHpr declHpr = decl.getHpr();
        jd = declHpr.getMethodDecl(meth);
    }

    // Handle Constructor
    else if(anObj instanceof Constructor) { Constructor constr = (Constructor)anObj; Class cls = constr.getDeclaringClass();
        JavaDecl decl = getClassDecl(cls);
        JavaDeclHpr declHpr = decl.getHpr();
        jd = declHpr.getConstructorDecl(constr);
    }
    
    // Handle JVarDecl
    else if(anObj instanceof JVarDecl)  { JVarDecl vd = (JVarDecl)anObj;
        JClassDecl cd = vd.getParent(JClassDecl.class);
        JavaDecl decl = cd.getDecl();
        jd = new JavaDecl(this, decl, vd);
    }
    
    // Handle Java.lang.refelect.Type
    else if(anObj instanceof Type) { Type type = (Type)anObj; //Class cls = getClass(type);
        try { jd = getTypeDecl(type, null); }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    // Complain
    else throw new RuntimeException("JavaDeclOwner.getJavaDecl: Unsupported type " + anObj);
    
    if(jd==null)
        System.out.println("JavaDecl.getJavaDecl: Decl not found for " + anObj);
    return jd;
}

/**
 * Returns a JavaDecl for type.
 */
public JavaDecl getTypeDecl(Type aType, JavaDecl aPar)
{
    String name = getTypeName(aType);
    JavaDecl decl = _decls.get(name); if(decl!=null) return decl;

    // Handle ParameterizedType
    if(aType instanceof ParameterizedType)
        decl = new JavaDecl(this, null, aType);
        
    // Handle TypeVariable
    else if(aType instanceof TypeVariable) { TypeVariable tv = (TypeVariable)aType;
        decl = aPar.getTypeVar(name);
        return decl;
    }
        
    // Handle Class
    else {
        Class cls = getClass(aType);
        return getClassDecl(cls);
    }
    
    _decls.put(name, decl);
    return decl;
}

/**
 * Returns a class decl.
 */
private JavaDecl getClassDecl(Class aClass)
{
    String cname = aClass.getName();
    JavaDecl decl = _decls.get(cname);
    if(decl==null) {
        _decls.put(cname, decl = createClassDecl(aClass));
        if(aClass.isArray())
            _decls.put(getClassName(aClass), decl);
    }
    return decl;
}

/**
 * Creates a class decl.
 */
private JavaDecl createClassDecl(Class aClass)
{
    // If declaring class, get decl from parent decl
    Class dcls = aClass.getDeclaringClass();
    if(dcls!=null) {
        JavaDecl parDecl = getJavaDecl(dcls);
        JavaDeclHpr declHpr = parDecl.getHpr();
        JavaDecl decl = declHpr.getClassDecl(aClass);
        if(decl==null) System.err.println("JavaDeclOwner.createClassDecl: Inner class not found " + aClass);
        return decl;
    }
    
    // Get parent decl
    JavaDecl parDecl = null;
    Package pkg = aClass.getPackage();
    String pname = pkg!=null? pkg.getName() : null;
    if(pname!=null && pname.length()>0)
        parDecl = getPackageDecl(pname);
    
    // Create and add JavaDecl for class
    return new JavaDecl(this, parDecl, aClass);
}

/**
 * Returns a package decl.
 */
private JavaDecl getPackageDecl(String aName)
{
    if(aName==null || aName.length()==0) return null;  // If bogus package name, just return
    JavaDecl pdecl = _decls.get(aName);
    if(pdecl==null) _decls.put(aName, createPackageDecl(aName));
    return pdecl;
}

/**
 * Creates a package decl.
 */
private JavaDecl createPackageDecl(String aName)
{
    // Get parent decl
    JavaDecl parDecl = null;
    int ind = aName.lastIndexOf('.');
    if(ind>=0) { String pname = aName.substring(0,ind);
        parDecl = getPackageDecl(pname); }
        
    // Create new decl
    JavaDecl pdecl = new JavaDecl(this, parDecl, aName);
    _decls.put(aName, pdecl);
    return pdecl;
}

/**
 * Returns the ClassLoader.
 */
public abstract ClassLoader getClassLoader();

/**
 * Returns a Class for given name.
 */
public Class getClass(String aName)
{
    ClassLoader cldr = getClassLoader();
    Class cls = ClassUtils.getClass(aName, cldr);
    return cls;
}

/**
 * Returns the class name, converting primitive arrays to 'int[]' instead of '[I'.
 */
public static String getTypeName(Type aType)
{
    // Handle Class
    if(aType instanceof Class) { Class cls = (Class)aType;
        return cls.getName(); }

    // Handle GenericArrayType
    if(aType instanceof GenericArrayType) { GenericArrayType gat = (GenericArrayType)aType;
        return getTypeName(gat.getGenericComponentType()) + "[]"; }
        
    // Handle ParameterizedType (e.g., Class <T>, List <T>, Map <K,V>)
    if(aType instanceof ParameterizedType) { ParameterizedType pt = (ParameterizedType)aType;
        Type base = pt.getRawType();
        StringBuffer sb = new StringBuffer(getTypeName(base)).append('<');
        Type types[] = pt.getActualTypeArguments(), last = types[types.length-1];
        for(Type ta : types) { sb.append(getTypeName(ta)); if(ta!=last) sb.append(','); }
        return sb.append('>').toString();
    }
        
    // Handle TypeVariable
    if(aType instanceof TypeVariable) { TypeVariable tv = (TypeVariable)aType;
        //Type typ = tv.getBounds()[0]; return getTypeName(typ);
        return tv.getName();
    }
        
    // Handle WildcardType
    if(aType instanceof WildcardType) { WildcardType wc = (WildcardType)aType;
        if(wc.getLowerBounds().length>0)
            return getTypeName(wc.getLowerBounds()[0]);
        return getTypeName(wc.getUpperBounds()[0]);
    }
    
    // Complain about anything else
    throw new RuntimeException("JavaDecl: Can't get Type name from type: " + aType);
}

/**
 * Returns the class simple name, converting primitive arrays to 'int[]' instead of '[I'.
 */
public static String getTypeSimpleName(Type aType)
{
    // Handle Class
    if(aType instanceof Class) { Class cls = (Class)aType;
        return cls.getSimpleName(); }

    // Handle GenericArrayType
    if(aType instanceof GenericArrayType) { GenericArrayType gat = (GenericArrayType)aType;
        return getTypeSimpleName(gat.getGenericComponentType()) + "[]"; }
        
    // Handle ParameterizedType (e.g., Class <T>, List <T>, Map <K,V>)
    if(aType instanceof ParameterizedType) { ParameterizedType pt = (ParameterizedType)aType;
        Type base = pt.getRawType();
        StringBuffer sb = new StringBuffer(getTypeSimpleName(base)).append('<');
        Type types[] = pt.getActualTypeArguments(), last = types[types.length-1];
        for(Type ta : types) { sb.append(getTypeSimpleName(ta)); if(ta!=last) sb.append(','); }
        return sb.append('>').toString();
    }
        
    // Handle TypeVariable
    if(aType instanceof TypeVariable) { TypeVariable tv = (TypeVariable)aType;
        return tv.getName(); }
        
    // Handle WildcardType
    if(aType instanceof WildcardType) { WildcardType wc = (WildcardType)aType;
        if(wc.getLowerBounds().length>0)
            return getTypeSimpleName(wc.getLowerBounds()[0]);
        return getTypeSimpleName(wc.getUpperBounds()[0]);
    }
    
    // Complain about anything else
    throw new RuntimeException("JavaDecl: Can't get Type name from type: " + aType);
}

/**
 * Returns the class name, converting primitive arrays to 'int[]' instead of '[I'.
 */
public static Class getClass(Type aType)
{
    // Handle Class
    if(aType instanceof Class)
        return (Class)aType;

    // Handle GenericArrayType
    if(aType instanceof GenericArrayType) { GenericArrayType gat = (GenericArrayType)aType;
        Class cls = getClass(gat.getGenericComponentType());
        return Array.newInstance(cls,0).getClass();
    }
        
    // Handle ParameterizedType (e.g., Class <T>, List <T>, Map <K,V>)
    if(aType instanceof ParameterizedType)
        return getClass(((ParameterizedType)aType).getRawType());
        
    // Handle TypeVariable
    if(aType instanceof TypeVariable)
        return getClass(((TypeVariable)aType).getBounds()[0]);
        
    // Handle WildcardType
    if(aType instanceof WildcardType) { WildcardType wc = (WildcardType)aType;
        if(wc.getLowerBounds().length>0)
            return getClass(wc.getLowerBounds()[0]);
        return getClass(wc.getUpperBounds()[0]);
    }
    
    // Complain about anything else
    throw new RuntimeException("JavaDecl: Can't get Type name from type: " + aType);
}

/**
 * Returns the class name, converting primitive arrays to 'int[]' instead of '[I'.
 */
public static String getClassName(Type aType)
{
    Class cls = getClass(aType);
    if(cls.isArray())
        return cls.getComponentType().getName() + "[]";
    return cls.getName();
}

/**
 * Returns an id string for given Java part.
 */
public static String getId(Object anObj)
{
    // Handle Class: <Name>
    if(anObj instanceof Class) { Class cls = (Class)anObj;
        return cls.getName(); }
    
    // Create StringBuffer
    StringBuffer sb = new StringBuffer();
    
    // Handle Field: DeclClassName.<Name>
    if(anObj instanceof Field) { Field field = (Field)anObj;
        sb.append(field.getDeclaringClass()).append('.').append(field.getName());
    }
    
    // Handle Method: DeclClassName.Name(<ParamType>,...)
    else if(anObj instanceof Method) { Method meth = (Method)anObj;
        sb.append(meth.getDeclaringClass()).append('.').append(meth.getName()).append('(');
        Class ptypes[] = meth.getParameterTypes(), last = ptypes.length>0? ptypes[ptypes.length-1] : null;
        for(Class ptype : ptypes) { sb.append(ptype.getName()); if(ptype!=last) sb.append(','); }
        sb.append(')');
    }
    
    // Handle Constructor: DeclClassName(<ParamType>,...)
    else if(anObj instanceof Constructor) { Constructor constr = (Constructor)anObj;
        sb.append(constr.getDeclaringClass()).append('(');
        Class ptypes[] = constr.getParameterTypes(), last = ptypes.length>0? ptypes[ptypes.length-1] : null;
        for(Class ptype : ptypes) { sb.append(ptype.getName()); if(ptype!=last) sb.append(','); }
        sb.append(')');
    }
    
    // Handle ParameterizedType: RawType<TypeArg,...>
    else if(anObj instanceof ParameterizedType) { ParameterizedType pt = (ParameterizedType)anObj;
        sb.append(pt.getRawType()).append('<');
        Type types[] = pt.getActualTypeArguments(), last = types.length>0? types[types.length-1] : null;
        for(Type type : types) { sb.append(getId(type)); if(type!=last) sb.append(','); }
        sb.append('>');
    }
    
    // Handle TypeVariable: DeclType.Name
    else if(anObj instanceof TypeVariable) { TypeVariable tv = (TypeVariable)anObj;
        sb.append(getId(tv.getGenericDeclaration())).append('.').append(tv.getName()); }
    
    // Handle GenericArrayType: CompType[]
    else if(anObj instanceof GenericArrayType) { GenericArrayType gat = (GenericArrayType)anObj;
        sb.append(gat.getGenericComponentType()).append("[]"); }
        
    // Handle WildcardType
    else if(anObj instanceof WildcardType) { WildcardType wc = (WildcardType)anObj;
        if(wc.getLowerBounds().length>0)
            sb.append(wc.getLowerBounds()[0]);
        else sb.append(wc.getUpperBounds()[0]);
    }
    
    // Handle JVarDecl
    else if(anObj instanceof JVarDecl) { JVarDecl vd = (JVarDecl)anObj;
        sb.append("JVarDecl").append(vd.getName()); }
        
    // Handle String (package name)
    else if(anObj instanceof String)
        sb.append(anObj);
    
    // Complain about anything else
    else throw new RuntimeException("Unsupported type");
    
    // Return string
    return sb.toString();
}

/**
 * Returns reference nodes in given JNode that match given JavaDecl.
 */
public static void getMatches(JNode aNode, JavaDecl aDecl, List <JNode> theMatches)
{
    // If JType check name
    if(aNode instanceof JType || aNode instanceof JExprId) {
        JavaDecl decl = isPossibleMatch(aNode, aDecl)? aNode.getDecl() : null;
        if(decl!=null && decl.matches(aDecl))
            theMatches.add(aNode);
    }
 
    // Recurse
    for(JNode child : aNode.getChildren())
        getMatches(child, aDecl, theMatches);
}
    
/**
 * Returns reference nodes in given JNode that match given JavaDecl.
 */
public static void getRefMatches(JNode aNode, JavaDecl aDecl, List <JNode> theMatches)
{
    // If JType check name
    if(aNode instanceof JType || aNode instanceof JExprId) {
        if(isPossibleMatch(aNode, aDecl) && !aNode.isDecl()) {
            JavaDecl decl = aNode.getDecl();
            if(decl!=null && decl.matches(aDecl) && aNode.getParent(JImportDecl.class)==null)
                theMatches.add(aNode);
        }
    }
 
    // Recurse
    for(JNode child : aNode.getChildren())
        getRefMatches(child, aDecl, theMatches);
}
    
/**
 * Returns declaration nodes in given JNode that match given JavaDecl.
 */
public static JNode getDeclMatch(JNode aNode, JavaDecl aDecl)
{
    List <JNode> matches = new ArrayList(); getDeclMatches(aNode, aDecl, matches);
    return matches.size()>0? matches.get(0) : null;
}

/**
 * Returns declaration nodes in given JNode that match given JavaDecl.
 */
public static void getDeclMatches(JNode aNode, JavaDecl aDecl, List <JNode> theMatches)
{
    // If JType check name
    if(aNode instanceof JType || aNode instanceof JExprId) {
        JavaDecl decl = aNode.isDecl() && isPossibleMatch(aNode, aDecl)? aNode.getDecl() : null;
        if(decl!=null && decl.matches(aDecl))
            theMatches.add(aNode);
    }
 
    // Recurse
    for(JNode child : aNode.getChildren())
        getDeclMatches(child, aDecl, theMatches);
}
    
/** Returns whether node is a possible match. */
private static boolean isPossibleMatch(JNode aNode, JavaDecl aDecl)
{
    if(aNode instanceof JType) { JType type = (JType)aNode;
        if(type.getSimpleName().equals(aDecl.getSimpleName()))
            return true; }
    else if(aNode instanceof JExprId) { JExprId id = (JExprId)aNode;
        if(id.getName().equals(aDecl.getSimpleName()))
            return true; }
    return false;
}

/**
 * Returns a simple class name.
 */
public static String getSimpleName(String cname)
{
    int i = cname.lastIndexOf('$'); if(i<0) i = cname.lastIndexOf('.'); if(i>0) cname = cname.substring(i+1);
    return cname;
}

}