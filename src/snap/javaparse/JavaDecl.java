/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snap.javaparse;
import java.lang.reflect.*;
import java.util.*;
import snap.project.Project;
import snap.util.*;

/**
 * A class to represent a declaration of a Java Class, Method, Field or Constructor.
 */
public class JavaDecl implements Comparable<JavaDecl> {
    
    // The project that this decl belongs to
    Project        _proj;

    // The JavaDecl (class) that this decl belongs to
    JavaDecl       _par;
    
    // The JavaDecls (children: fields, methods, constructors) that belong to this JavaDecl (class)
    JavaDecls      _decls;
    
    // The type
    DeclType       _type;
    
    // The modifiers
    int            _mods;
    
    // The name of the declaration member
    String         _name;
    
    // The simple name of the declaration member
    String         _sname;
    
    // The class name of this declaration
    String         _cname;
    
    // Whether class decl is enum or interface
    boolean        _enum, _interface;
    
    // The type this decl evaluates to when referenced
    JavaDecl       _evalType;
    
    // The JavaDecls for arg types
    JavaDecl       _argTypes[];
    
    // The VariableDecl
    JVarDecl       _vdecl;
    
    // Constants for type
    public enum DeclType { Class, Field, Constructor, Method, Package, VarDecl }
    
/**
 * Creates a new JavaDecl for Class, Field, Constructor, Method, VarDecl or class name string.
 */
public JavaDecl(Project aProj, JavaDecl aPar, Object anObj)
{
    // Set JavaDecls
    _proj = aProj; _par = aPar; if(aProj==null && aPar!=null) _proj = aPar._proj;
    
    // Handle Class
    if(anObj instanceof Class) { Class cls = (Class)anObj; _type = DeclType.Class;
        _name = getClassName(cls); _sname = cls.getSimpleName(); _cname = _name;
        _mods = cls.getModifiers(); _enum = cls.isEnum(); _interface = cls.isInterface();
        _evalType = this;
        _decls = new JavaDecls(this);
    }

    // Handle Field
    else if(anObj instanceof Field) { Field field = (Field)anObj; _type = DeclType.Field;
        _name = _sname = field.getName(); _cname = field.getDeclaringClass().getName();
        _evalType = getJavaDecl(field.getGenericType()); _mods = field.getModifiers();
    }
    
    // Handle Method
    else if(anObj instanceof Method) { Method meth = (Method)anObj; _type = DeclType.Method;
        _name = _sname = meth.getName(); _cname = meth.getDeclaringClass().getName();
        _evalType = getJavaDecl(meth.getGenericReturnType()); _mods = meth.getModifiers();
        
        // Get GenericParameterTypes and names
        Type ptypes[] = meth.getGenericParameterTypes();
        _argTypes = new JavaDecl[meth.getParameterCount()];
        for(int i=0,iMax=ptypes.length; i<iMax; i++)
            _argTypes[i] = getJavaDecl(ptypes[i]);
    }
    
    // Handle Constructor
    else if(anObj instanceof Constructor) { Constructor constr = (Constructor)anObj; _type = DeclType.Constructor;
        Class dcls = constr.getDeclaringClass();
        _name = _cname = constr.getName(); _sname = dcls.getSimpleName();
        _mods = constr.getModifiers();
        _evalType = getJavaDecl(dcls);
    
        // Get GenericParameterTypes (this can fail https://bugs.openjdk.java.net/browse/JDK-8075483))
        Type ptypes[] = constr.getGenericParameterTypes();
        if(ptypes.length<constr.getParameterCount()) ptypes = constr.getParameterTypes();
        _argTypes = new JavaDecl[ptypes.length];
        for(int i=0,iMax=ptypes.length; i<iMax; i++)
            _argTypes[i] = getJavaDecl(ptypes[i]);
    }
    
    // Handle VarDecl
    else if(anObj instanceof JVarDecl) { _vdecl = (JVarDecl)anObj; _type = DeclType.VarDecl;
        _name = _sname = _vdecl.getName();
        JType jt = _vdecl.getType();
        _evalType = jt!=null? jt.getDecl() : getJavaDecl(Object.class); // Can happen for Lambdas
    }
    
    // Handle Package String
    else if(anObj instanceof String) { String pname = (String)anObj; _type = DeclType.Package;
        _name = pname; _sname = getSimpleName(pname); }
    
    // Throw exception for unknown type
    else throw new RuntimeException("JavaDecl.init: Unsupported type " + anObj);
}

/**
 * Returns the type.
 */
public DeclType getType()  { return _type; }

/**
 * Returns whether is a class reference.
 */
public boolean isClass()  { return _type==DeclType.Class; }

/**
 * Returns whether is a enum reference.
 */
public boolean isEnum()  { return _type==DeclType.Class && _enum; }

/**
 * Returns whether is a interface reference.
 */
public boolean isInterface()  { return _type==DeclType.Class && _interface; }

/**
 * Returns whether is a field reference.
 */
public boolean isField()  { return _type==DeclType.Field; }

/**
 * Returns whether is a constructor reference.
 */
public boolean isConstructor()  { return _type==DeclType.Constructor; }

/**
 * Returns whether is a method reference.
 */
public boolean isMethod()  { return _type==DeclType.Method; }

/**
 * Returns whether is a package reference.
 */
public boolean isPackage()  { return _type==DeclType.Package; }

/**
 * Returns whether is a variable declaration reference.
 */
public boolean isVarDecl()  { return _type==DeclType.VarDecl; }

/**
 * Returns the modifiers.
 */
public int getModifiers()  { return _mods; }

/**
 * Returns whether decl is static.
 */
public boolean isStatic()  { return Modifier.isStatic(_mods); }

/**
 * Returns the name.
 */
public String getName()  { return _name; }

/**
 * Returns the simple name.
 */
public String getSimpleName()  { return _sname; }

/**
 * Returns the class name.
 */
public String getClassName()  { return _cname; }

/**
 * Returns the class name.
 */
public String getClassSimpleName()  { return getSimpleName(_cname); }

/**
 * Returns the parent class name.
 */
public String getParentClassName()  { return _cname!=null? getParentClassName(_cname) : null; }

/**
 * Returns the top level class name.
 */
public String getRootClassName()  { return _cname!=null? getRootClassName(_cname) : null; }

/**
 * Returns whether class is member.
 */
public boolean isMemberClass()  { return _cname!=null? _cname.indexOf('$')>0 : false; }

/**
 * Returns the JavaDecls for class.
 */
public JavaDecls getDecls()  { return _decls; }

/**
 * Returns the enclosing class this decl.
 */
public JavaDecl getParent()
{
    JavaDecl par = _decls!=null? _decls._cdecl : null;
    return par!=this? par : null;
}

/**
 * Returns the JavaDecl for class this decl evaluates to when referenced.
 */
public JavaDecl getEvalType()  { return _evalType; }

/**
 * Returns the class this decl evaluates to when referenced.
 */
public Class getEvalClass()
{
    if(_evalType==null) return null;
    if(_evalType!=this) return _evalType.getEvalClass();
    ClassLoader cldr = _proj!=null? _proj.getClassLoader() : ClassLoader.getSystemClassLoader();
    String cname = getEvalTypeName();
    return ClassUtils.getClass(cname, cldr);
}

/**
 * Returns the type name for class this decl evaluates to when referenced.
 */
public String getEvalTypeName()  { return _evalType.getName(); }

/**
 * Returns the arg types.
 */
public JavaDecl[] getArgTypes()  { return _argTypes; }

/**
 * Returns the arg type names.
 */
public String[] getArgTypeNames()
{
    String names[] = new String[_argTypes.length];
    for(int i=0;i<names.length;i++) names[i] = _argTypes[i].getName();
    return names;
}

/**
 * Returns the arg type simple names.
 */
public String[] getArgTypeSimpleNames()
{
    String names[] = new String[_argTypes.length];
    for(int i=0;i<names.length;i++) names[i] = _argTypes[i].getSimpleName();
    return names;
}

/**
 * Returns the package name.
 */
public String getPackageName() { return isPackage()? _name : getPackageName(getClassName()); }

/**
 * Returns the variable declaration name.
 */
public JVarDecl getVarDecl() { return _vdecl; }

/**
 * Returns a name suitable to describe declaration.
 */
public String getPrettyName()
{
    String name = _cname;
    if(isMethod() || isField()) name += '.' + _name;
    if(isMethod() || isConstructor()) name +=  '(' + StringUtils.join(getArgTypeSimpleNames(), ",") + ')';
    if(isPackage()) return _name;
    if(isVarDecl()) return _name;
    return name;
}

/**
 * Returns a name unique for matching declarations.
 */
public String getMatchName()
{
    String name = _cname;
    if(isMethod() || isField()) name += '.' + _name;
    if(isMethod() || isConstructor()) name +=  '(' + StringUtils.join(getArgTypeNames(), ",") + ')';
    if(isPackage()) return _name;
    if(isVarDecl()) return _name;
    return name;
}

/**
 * Returns the full name.
 */
public String getFullName()
{
    if(_fname!=null) return _fname;
    String name = getMatchName();
    if(isMethod() || isField()) name = getEvalTypeName() + " " + name;
    String mstr = Modifier.toString(_mods); if(mstr.length()>0) name = mstr + " " + name;
    return _fname=name;
} String _fname;

/**
 * Returns a string representation of suggestion.
 */
public String getSuggestionString()
{
    StringBuffer sb = new StringBuffer(getSimpleName());
    switch(getType()) {
        case Constructor:
        case Method: sb.append('(').append(StringUtils.join(getArgTypeSimpleNames(), ",")).append(')');
        case VarDecl: case Field:
            if(getEvalType()!=null) sb.append(" : ").append(getEvalType().getSimpleName());
            if(getClassName()!=null) sb.append(" - ").append(getClassSimpleName());
            break;
        case Class: sb.append(" - ").append(getParentClassName()); break;
        case Package: break;
        default:  throw new RuntimeException("Unsupported Type " + getType());
    }

    // Return string
    return sb.toString();
}

/**
 * Returns the string to use when inserting this suggestion into code.
 */
public String getReplaceString()
{
    switch(getType()) {
        case Class: return getSimpleName();
        case Constructor: return getPrettyName().replace(getParentClassName() + '.', "");
        case Method: return getPrettyName().replace(_cname + '.', "");
        case Package: {
            String name = getPackageName(); int index = name.lastIndexOf('.');
            return index>0? name.substring(index+1) : name;
        }
        default: return getName();
    }
}

/**
 * Returns the class or declaring class using the given project.
 */
public Class getDeclClass()
{
    ClassLoader cldr = _proj!=null? _proj.getClassLoader() : ClassLoader.getSystemClassLoader();
    return ClassUtils.getClass(_cname, cldr);
}

/**
 * Returns a JavaDecl for given object.
 */
public JavaDecl getJavaDecl(Object anObj)
{
    if(_proj==null)
        return null;
    return _proj.getJavaDecl(anObj);
}

/**
 * Returns whether given declaration collides with this declaration.
 */
public boolean matches(JavaDecl aDecl)
{
    if(aDecl==this) return true;
    if(aDecl._type!=_type) return false;
    if(!aDecl._name.equals(_name)) return false;
    if(!Arrays.equals(aDecl._argTypes, _argTypes)) return false;
    
    // If field or method, see if declaring class matches
    if(isField() || isConstructor() || isMethod()) {
        Class c1 = getDeclClass(), c2 = aDecl.getDeclClass();
        return c1==c2 || c1.isAssignableFrom(c2) || c2.isAssignableFrom(c1);
    }
    
    return true;
}

/**
 * Standard compareTo implementation.
 */
public int compareTo(JavaDecl aDecl)
{
    int t1 = _type.ordinal(), t2 = aDecl._type.ordinal();
    if(t1<t2) return -1; if(t2<t1) return 1;
    return getMatchName().compareTo(aDecl.getMatchName());
}

/**
 * Standard hashcode implementation.
 */
public int hashCode()  { return getFullName().hashCode(); }

/**
 * Standard toString implementation.
 */
public String toString()  { return _type + ": " + getFullName(); }

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

/** Returns a simple class name. */
public static String getSimpleName(String cname)
{
    int i = cname.lastIndexOf('$'); if(i<0) i = cname.lastIndexOf('.'); if(i>0) cname = cname.substring(i+1);
    return cname;
}

/** Returns the parent class name. */
private static String getParentClassName(String cname)
{
   int i = cname.lastIndexOf('$'); if(i<0) i = cname.lastIndexOf('.'); if(i>0) cname = cname.substring(0,i);
   return cname;
}

/** Returns the top level class name. */
private static String getRootClassName(String cname)
{
   int i = cname.indexOf('$'); if(i>0) cname = cname.substring(0,i);
   return cname;
}

/** Returns a package name for a class name. */
private static String getPackageName(String cname)
{
    int i = cname.lastIndexOf('.'); cname = i>0? cname.substring(0,i) : "";
    return cname;
}

/**
 * Returns a JavaDecl for object.
 */
public static JavaDecl getJavaDecl(Project aProj, Object anObj)
{
    // Handle Class
    JavaDecl jd = null;
    if(anObj instanceof Class) { Class cls = (Class)anObj;
        JavaDecl decl = aProj.getJavaDecl(cls);
        JavaDecls decls = decl.getDecls();
        jd = decls.getClassDecl();
    }
    
    // Handle Field
    else if(anObj instanceof Field) { Field field = (Field)anObj; Class cls = field.getDeclaringClass();
        JavaDecl decl = aProj.getJavaDecl(cls);
        JavaDecls decls = decl.getDecls();
        jd = decls.getFieldDecl(field);
    }
    
    // Handle Method
    else if(anObj instanceof Method) { Method meth = (Method)anObj; Class cls = meth.getDeclaringClass();
        JavaDecl decl = aProj.getJavaDecl(cls);
        JavaDecls decls = decl.getDecls();
        jd = decls.getMethodDecl(meth);
    }

    // Handle Constructor
    else if(anObj instanceof Constructor) { Constructor constr = (Constructor)anObj; Class cls = constr.getDeclaringClass();
        JavaDecl decl = aProj.getJavaDecl(cls);
        JavaDecls decls = decl.getDecls();
        jd = decls.getConstructorDecl(constr);
    }
    
    // Handle JVarDecl
    else if(anObj instanceof JVarDecl)  { JVarDecl vd = (JVarDecl)anObj;
        JClassDecl cd = vd.getParent(JClassDecl.class);
        JavaDecl decl = cd.getDecl();
        jd = new JavaDecl(aProj, decl, vd);
    }
    
    // Handle Java.lang.refelect.Type
    else if(anObj instanceof Type) { Type type = (Type)anObj;
        Class cls = JavaDecl.getClass(type);
        jd = aProj.getJavaDecl(cls);
    }

    // Complain
    else throw new RuntimeException("Project.getJavaDecl: Unsupported type " + anObj);
    
    if(jd==null)
        System.out.println("JavaDecl.getJavaDecl: Decl not found for " + anObj);
    return jd;
}

}