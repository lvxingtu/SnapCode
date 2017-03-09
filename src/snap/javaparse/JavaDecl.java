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
    
    // The JavaDecls that this decl belongs to
    JavaDecls      _decls;
    
    // The project that this decl belongs to
    Project        _proj;

    // The type
    Type           _type;
    
    // The modifiers
    int            _mods;
    
    // The name of the declaration member
    String         _name;
    
    // The simple name of the declaration member
    String         _sname;
    
    // The class name of this declaration
    String         _cname;
    
    // The type of the declaration member
    String         _tname;
    
    // A string description of arg types
    String         _argTypeNames[];
    
    // The package name
    String         _pname;
    
    // The VariableDecl
    JVarDecl       _vdecl;
    
    // Constants for type
    public enum Type { Class, Field, Constructor, Method, Package, VarDecl }
    
    // Common shared decls
    public static final JavaDecl INT_DECL = new JavaDecl(null, boolean.class);
    public static final JavaDecl BOOL_DECL = new JavaDecl(null, boolean.class);
    public static final JavaDecl OBJECT_DECL = new JavaDecl(null, Object.class);
    public static final JavaDecl CLASS_DECL = new JavaDecl(null, Class.class);
    public static final JavaDecl STRING_DECL = new JavaDecl(null, String.class);
    
/**
 * Creates a new JavaDecl for Class, Field, Constructor, Method, VarDecl or class name string.
 */
public JavaDecl(JavaDecls theJDs, Object anObj)
{
    // Set JavaDecls
    _decls = theJDs; _proj = theJDs!=null? theJDs.getProject() : null;
    
    // Handle Class
    if(anObj instanceof Class) { Class cls = (Class)anObj; _type = Type.Class;
        _name = getTypeName(cls); _sname = cls.getSimpleName(); _cname = _name;
        _mods = cls.getModifiers();
    }

    // Handle Field
    else if(anObj instanceof Field) { Field field = (Field)anObj; _type = Type.Field;
        _name = _sname = field.getName(); _cname = field.getDeclaringClass().getName();
        _tname = getTypeName(field.getGenericType()); _mods = field.getModifiers();
    }
    
    // Handle Method
    else if(anObj instanceof Method) { Method meth = (Method)anObj; _type = Type.Method;
        _name = _sname = meth.getName(); _cname = meth.getDeclaringClass().getName();
        _tname = getTypeName(meth.getGenericReturnType()); _mods = meth.getModifiers();
        
        // Get GenericParameterTypes and names
        java.lang.reflect.Type ptypes[] = meth.getGenericParameterTypes();
        _argTypeNames = new String[meth.getParameterCount()];
        for(int i=0,iMax=ptypes.length; i<iMax; i++)
            _argTypeNames[i] = getTypeName(ptypes[i]);
    }
    
    // Handle Constructor
    else if(anObj instanceof Constructor) { Constructor constr = (Constructor)anObj; _type = Type.Constructor;
        _name = _cname = _tname = constr.getName();
        _sname = constr.getDeclaringClass().getSimpleName();
        _mods = constr.getModifiers();
    
        // Get GenericParameterTypes (this can fail https://bugs.openjdk.java.net/browse/JDK-8075483))
        java.lang.reflect.Type ptypes[] = constr.getGenericParameterTypes();
        if(ptypes.length<constr.getParameterCount()) ptypes = constr.getParameterTypes();
        _argTypeNames = new String[ptypes.length];
        for(int i=0,iMax=ptypes.length; i<iMax; i++)
            _argTypeNames[i] = getTypeName(ptypes[i]);
    }
    
    // Handle VarDecl
    else if(anObj instanceof JVarDecl) { _vdecl = (JVarDecl)anObj; _type = Type.VarDecl;
        _name = _sname = _vdecl.getName(); _tname = _vdecl.getClassName();
    }
    
    // Handle String (assumed to be class name)
    else if(anObj instanceof String) { String cname = (String)anObj; _type = Type.Class;
        _name = cname; _sname = getSimpleName(cname); _cname = cname;
    }
    
    // Throw exception for unknown type
    else throw new RuntimeException("JavaDecl.init: Unsupported type " + anObj);
}

/**
 * Returns the type.
 */
public Type getType()  { return _type; }

/**
 * Returns whether is a class reference.
 */
public boolean isClass()  { return _type==Type.Class; }

/**
 * Returns whether is a field reference.
 */
public boolean isField()  { return _type==Type.Field; }

/**
 * Returns whether is a constructor reference.
 */
public boolean isConstructor()  { return _type==Type.Constructor; }

/**
 * Returns whether is a method reference.
 */
public boolean isMethod()  { return _type==Type.Method; }

/**
 * Returns whether is a package reference.
 */
public boolean isPackage()  { return _type==Type.Package; }

/**
 * Returns whether is a variable declaration reference.
 */
public boolean isVarDecl()  { return _type==Type.VarDecl; }

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
 * Returns the type name (for method or field).
 */
public String getTypeName()  { return _tname; }

/**
 * Returns the simple type name.
 */
public String getTypeSimpleName()  { return getSimpleName(_tname); }

/**
 * Returns the package name.
 */
public String getPackageName() { return _pname!=null? _pname : (_pname=getPackageName(_cname)); }

/**
 * Returns the variable declaration name.
 */
public JVarDecl getVarDecl() { assert(isVarDecl()); return _vdecl; }

/**
 * Returns a name suitable to describe declaration.
 */
public String getPrettyName()
{
    String name = _cname;
    if(isMethod() || isField()) name += '.' + _name;
    if(isMethod() || isConstructor()) {
        String names[] = Arrays.copyOf(_argTypeNames, _argTypeNames.length);
        for(int i=0;i<names.length;i++) names[i] = getSimpleName(names[i]);
        name +=  '(' + StringUtils.join(names, ",") + ')';
    }
    if(isPackage()) return _pname;
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
    if(isMethod() || isConstructor()) name +=  '(' + StringUtils.join(_argTypeNames, ",") + ')';
    if(isPackage()) return _pname;
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
    if(isMethod() || isField()) name = _tname + " " + name;
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
        case Method: {
            String names[] = Arrays.copyOf(_argTypeNames, _argTypeNames.length);
            for(int i=0;i<names.length;i++) names[i] = getSimpleName(names[i]);
            sb.append('(').append(StringUtils.join(names, ",")).append(')');
        }
        case VarDecl: case Field:
            if(getTypeName()!=null) sb.append(" : ").append(getTypeSimpleName());
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
 * Returns whether given declaration collides with this declaration.
 */
public boolean matches(JavaDecl aDecl)
{
    if(aDecl==this) return true;
    if(aDecl._type!=_type) return false;
    if(!aDecl._name.equals(_name)) return false;
    if(!Arrays.equals(aDecl._argTypeNames, _argTypeNames)) return false;
    
    // If field or method, see if declaring class matches
    if(isField() || isConstructor() || isMethod()) {
        Class c1 = getDeclClass(), c2 = aDecl.getDeclClass();
        return c1==c2 || c1.isAssignableFrom(c2) || c2.isAssignableFrom(c1);
    }
    
    return true;
}

/**
 * Standard equals implementation.
 */
public boolean equals(Object anObj)
{
    if(anObj==this) return true;
    JavaDecl decl = anObj instanceof JavaDecl? (JavaDecl)anObj : null; if(decl==null) return false;
    if(decl._type!=_type) return false;
    if(decl._mods!=_mods) return false;
    if(!decl._name.equals(_name)) return false;
    if(!SnapUtils.equals(decl._cname,_cname)) return false;
    if(_tname!=null && !_tname.equals(decl._tname)) return false;
    if(!Arrays.equals(decl._argTypeNames, _argTypeNames)) return false;
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
public String toString()  { return getFullName(); }

/**
 * Returns the class name, converting primitive arrays to 'int[]' instead of '[I'.
 */
public static String getTypeName(java.lang.reflect.Type aType)
{
    // Handle Class
    if(aType instanceof Class) { Class cls = (Class)aType;
        if(cls.isArray()) return getTypeName(cls.getComponentType()) + "[]";
        return cls.getName();
    }
        
    // Handle GenericArrayType
    if(aType instanceof GenericArrayType) { GenericArrayType gat = (GenericArrayType)aType;
        return getTypeName(gat.getGenericComponentType()) + "[]"; }
        
    // Handle ParameterizedType (e.g., Class <T>, List <T>, Map <K,V>)
    if(aType instanceof ParameterizedType)
        return getTypeName(((ParameterizedType)aType).getRawType());
        
    // Handle TypeVariable
    if(aType instanceof TypeVariable)
        return getTypeName(((TypeVariable)aType).getBounds()[0]);
        
    // Handle WildcardType
    if(aType instanceof WildcardType) { WildcardType wc = (WildcardType)aType;
        if(wc.getLowerBounds().length>0)
            return getTypeName(wc.getLowerBounds()[0]);
        return getTypeName(wc.getUpperBounds()[0]);
    }
    
    // Complain about anything else
    throw new RuntimeException("JavaDecl: Can't get Type name from type: " + aType);
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

}