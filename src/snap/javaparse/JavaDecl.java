/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snap.javaparse;
import java.lang.reflect.*;
import java.util.*;
import snap.util.*;

/**
 * A class to represent a declaration of a Java Class, Method, Field or Constructor.
 */
public class JavaDecl implements Comparable<JavaDecl> {
    
    // The JavaDeclOwner that this decl belongs to
    JavaDeclOwner  _owner;

    // The JavaDecl (class) that this decl was declared in
    JavaDecl       _par;
    
    // A unique identifier
    String         _id;
    
    // The type
    DeclType       _type;
    
    // The modifiers
    int            _mods;
    
    // The name of the declaration member
    String         _name;
    
    // The simple name of the declaration member
    String         _sname;
    
    // Whether class decl is enum or interface
    boolean        _enum, _interface, _primitive;
    
    // Whether method is VarArgs
    boolean        _varArgs;
    
    // The type this decl evaluates to when referenced
    JavaDecl       _evalType;
    
    // The JavaDecls for arg types for Constructor, Method
    JavaDecl       _argTypes[];
    
    // The JavaDecls for TypeVarss for Class, Method
    JavaDecl       _typeVars[] = EMPTY_DECLS;
    
    // The VariableDecl
    JVarDecl       _vdecl;
    
    // The Array item type (if Array)
    JavaDecl       _arrayItemType;
    
    // The super implementation of this type (Class, Method, Constructor)
    JavaDecl       _sdecl = NULL_DECL; String _scn;
    
    // The JavaDeclHpr to access children of this class JavaDecl (fields, methods, constructors, inner classes)
    JavaDeclHpr    _hpr;
    
    // Constants for type
    public enum DeclType { Class, Field, Constructor, Method, Package, VarDecl, ParamType, TypeVar }
    
    // Shared empty TypeVar array
    private static JavaDecl NULL_DECL = new JavaDecl(null, null, "NULL_DECL");
    private static JavaDecl[] EMPTY_DECLS = new JavaDecl[0];
    
/**
 * Creates a new JavaDecl for Class, Field, Constructor, Method, VarDecl or class name string.
 */
public JavaDecl(JavaDeclOwner anOwner, JavaDecl aPar, Object anObj)
{
    // Set JavaDecls
    _owner = anOwner; _par = aPar; assert(_owner!=null || anObj instanceof String);
    _id = JavaDeclOwner.getId(anObj);
    
    // Handle Type
    if(anObj instanceof Type)
        initType((Type)anObj);
    
    // Handle Member (Field, Method, Constructor)
    else if(anObj instanceof Member)
        initMember((Member)anObj);

    // Handle VarDecl
    else if(anObj instanceof JVarDecl) { _vdecl = (JVarDecl)anObj; _type = DeclType.VarDecl;
        _name = _sname = _vdecl.getName();
        JType jt = _vdecl.getType();
        _evalType = jt!=null? jt.getDecl() : getJavaDecl(Object.class); // Can happen for Lambdas
    }
    
    // Handle String
    else if(anObj instanceof String)
        initWithString((String)anObj);
    
    // Throw exception for unknown type
    else throw new RuntimeException("JavaDecl.init: Unsupported type " + anObj);
}

/**
 * Initialize types (Class, ParameterizedType, TypeVariable).
 */
private void initType(Type aType)
{
    // Handle ParameterizedType
    if(aType instanceof ParameterizedType) { ParameterizedType pt = (ParameterizedType)aType;
        _type = DeclType.ParamType;
        _name = JavaDeclOwner.getTypeName(pt); _sname = JavaDeclOwner.getTypeSimpleName(pt);
        _par = _owner.getTypeDecl(pt.getRawType(), _par);
        Type typArgs[] = pt.getActualTypeArguments();
        _argTypes = new JavaDecl[typArgs.length];
        for(int i=0,iMax=typArgs.length;i<iMax;i++) _argTypes[i] = _owner.getTypeDecl(typArgs[i], _par);
        _evalType = this;
    }
    
    // Handle TypeVariable
    else if(aType instanceof TypeVariable) { TypeVariable tv = (TypeVariable)aType; _type = DeclType.TypeVar;
        _name = _sname = tv.getName();
        Type etypes[] = tv.getBounds();
        _evalType = _owner.getTypeDecl(etypes[0], _par);
    }
    
    // Handle Class
    else if(aType instanceof Class) { Class cls = (Class)aType; _type = DeclType.Class;
        _mods = cls.getModifiers();
        _name = JavaDeclOwner.getClassName(cls); _sname = cls.getSimpleName();
        _enum = cls.isEnum(); _interface = cls.isInterface(); _primitive = cls.isPrimitive();
        _evalType = this;
        Class scls = cls.getSuperclass();
        _scn = scls!=null? scls.getName() : null;
        if(cls.isArray())
            _arrayItemType = getJavaDecl(cls.getComponentType());
    }
}

/**
 * Initialize member (Field, Method, Constructor).
 */
private void initMember(Member aMmbr)
{
    // Set mods, name, simple name
    _mods = aMmbr.getModifiers();
    _name = _sname = aMmbr.getName();
    
    // Handle Field
    if(aMmbr instanceof Field) { Field field = (Field)aMmbr; _type = DeclType.Field;
        _evalType = _owner.getTypeDecl(field.getGenericType(), _par); }
        
    // Handle Executable (Method, Constructor)
    else { Executable exec = (Executable)aMmbr; _type = exec instanceof Method? DeclType.Method : DeclType.Constructor;
        
        // Get TypeVars
        TypeVariable tvars[] = exec.getTypeParameters();
        _typeVars = new JavaDecl[tvars.length];
        for(int i=0,iMax=tvars.length;i<iMax;i++) _typeVars[i] = new JavaDecl(_owner,this,tvars[i]);
        _varArgs = exec.isVarArgs();
        
        // Get Return Type
        Type rtype = exec.getAnnotatedReturnType().getType();
        _evalType = _owner.getTypeDecl(rtype, this);
        
        // Get GenericParameterTypes (this can fail https://bugs.openjdk.java.net/browse/JDK-8075483))
        Type ptypes[] = exec.getGenericParameterTypes();
        if(ptypes.length<exec.getParameterCount()) ptypes = exec.getParameterTypes();
        _argTypes = new JavaDecl[ptypes.length];
        for(int i=0,iMax=ptypes.length; i<iMax; i++)
            _argTypes[i] = _owner.getTypeDecl(ptypes[i], this);
    }
}

/**
 * Initialize from String (Package, ParamType).
 */
private void initWithString(String aStr)
{
    // Handle ParamType string
    if(aStr.indexOf('<')>0) { _type = DeclType.ParamType;

        // Get parts from string
        String cname = aStr.substring(0,aStr.indexOf('<'));
        String pnamesStr = aStr.substring(aStr.indexOf('<')+1,aStr.length()-1);
        String pnames[] = pnamesStr.split(",");
        
        // Set parts
        _name = aStr; _par = getJavaDecl(cname);
        _argTypes = new JavaDecl[pnames.length];
        for(int i=0,iMax=pnames.length;i<iMax;i++) _argTypes[i] = getJavaDecl(pnames[i]);
        _evalType = this;
        
        // Build simple name
        _sname = _par.getSimpleName() + '<'; JavaDecl last = _argTypes[_argTypes.length-1];
        for(JavaDecl a : _argTypes) { _sname += a.getSimpleName(); if(a!=last) _sname += ','; }
        _sname += '>';
    }
        
    // Handle Package string
    else { _type = DeclType.Package; _name = aStr; _sname = JavaDeclOwner.getSimpleName(aStr); }
}

/**
 * Returns the id.
 */
public String getId()  { return _id; }

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
 * Returns whether is an array.
 */
public boolean isArray()  { return _arrayItemType!=null; }

/**
 * Returns the Array item type (if Array).
 */
public JavaDecl getArrayItemType()  { return _arrayItemType; }

/**
 * Returns whether is primitive.
 */
public boolean isPrimitive()  { return _type==DeclType.Class && _primitive; }

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
 * Returns whether is a parameterized class.
 */
public boolean isParamType()  { return _type==DeclType.ParamType; }

/**
 * Returns whether is a TypeVar.
 */
public boolean isTypeVar()  { return _type==DeclType.TypeVar; }

/**
 * Returns whether is Type is explicit (doesn't contain any type variables).
 */
public boolean isResolvedType()
{
    if(isTypeVar()) return false;
    if(isParamType()) {
        if(getParent().isTypeVar()) return false;
        for(JavaDecl tv : getTypeVars())
            if(tv.isTypeVar())
                return false;
    }
    return true;
}

/**
 * Returns whether is a Type (Class, ParamType, TypeVar).
 */
public boolean isType()  { return isClass() || isParamType() || isTypeVar(); }

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
 * Returns the type of the most basic class associated with this type:
 *     Class: itself
 *     Field, Method, Constructor, ParamType: Parent class
 *     TypeVar: EvalType.ClassType
 *     VarDecl, Package: null?
 */
public JavaDecl getClassType()
{
    if(isClass()) return this;
    if(isTypeVar()) return _evalType.getClassType();
    return _par!=null? _par.getClassType() : null;
}

/**
 * Returns the class name.
 */
public String getClassName()
{
    JavaDecl ct = getClassType();
    return ct!=null? ct.getName() : null;
}

/**
 * Returns the class simple name.
 */
public String getClassSimpleName()
{
    JavaDecl ct = getClassType();
    return ct!=null? ct.getSimpleName() : null;
}

/**
 * Returns the enclosing class this decl.
 */
public JavaDecl getParent()  { return _par; }

/**
 * Returns the enclosing class this decl.
 */
public JavaDecl getParent(DeclType aType)
{
    if(_par==null) return null;
    if(_par.getType()==aType) return _par;
    return _par.getParent(aType);
}

/**
 * Returns the parent class.
 */
public Class getParentClass()  { return _par!=null? _par.getEvalClass() : null; }

/**
 * Returns the parent class name.
 */
public String getParentClassName()  { return _par!=null? _par.getClassName() : null; }

/**
 * Returns the top level class name.
 */
public String getRootClassName()
{
    if(_par!=null && _par.isClass()) return _par.getRootClassName();
    if(isClass()) return getClassName();
    return null;
}

/**
 * Returns whether class is member.
 */
public boolean isMemberClass()  { return isClass() && _par!=null; }

/**
 * Returns the JavaDeclHpr for class child decls.
 */
public JavaDeclHpr getHpr()  { return _hpr!=null? _hpr : (_hpr = new JavaDeclHpr(this)); }

/**
 * Returns the JavaDecl for class this decl evaluates to when referenced.
 */
public JavaDecl getEvalType()  { return _evalType; }

/**
 * Returns the type name for class this decl evaluates to when referenced.
 */
public String getEvalTypeName()  { return _evalType.getName(); }

/**
 * Returns the type name for class this decl evaluates to when referenced.
 */
public String getEvalClassName()  { return _evalType.getClassName(); }

/**
 * Returns the class this decl evaluates to when referenced.
 */
public Class getEvalClass()
{
    String cname = getEvalClassName(); if(cname==null) return null;
    return _owner.getClass(cname);
}

/**
 * Returns the number of Method/ParamType parameters.
 */
public int getParamCount()  { return _argTypes.length; }

/**
 * Returns the individual Method parameter type at index.
 */
public JavaDecl getParamType(int anIndex)  { return _argTypes[anIndex]; }

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
 * Returns whether Method/Constructor is VarArgs type.
 */
public boolean isVarArgs()  { return _varArgs; }

/**
 * Returns the TypeVars.
 */
public JavaDecl[] getTypeVars()  { return _typeVars; }

/**
 * Returns the TypeVar with given name.
 */
public JavaDecl getTypeVar(String aName)
{
    if(isClass())
        return getHpr().getTypeVarDecl(aName);
        
    else if(isParamType()) {
        int ind = _par.getHpr().getTypeVarDeclIndex(aName);
        if(ind>=0 && ind<_argTypes.length)
            return _argTypes[ind];
    }
    
    else if(isMethod()) {
        for(JavaDecl tvar : _typeVars)
            if(tvar.getName().equals(aName))
                return tvar;
        return _par.getTypeVar(aName);
    }
    
    else if(isConstructor())
        return _par.getTypeVar(aName);

    return null;
}

/**
 * Returns the package decl.
 */
public JavaDecl getPackageDecl()
{
    if(isPackage()) return this;
    if(_par!=null) return _par.getPackageDecl();
    return null;
}

/**
 * Returns the package name.
 */
public String getPackageName() { JavaDecl pd = getPackageDecl(); return pd!=null? pd.getName() : null; }

/**
 * Returns the variable declaration name.
 */
public JVarDecl getVarDecl() { return _vdecl; }

/**
 * Returns the super decl of this JavaDecl (Class, Method, Constructor).
 */
public JavaDecl getSuper()
{
    // If already set, just return
    if(_sdecl!=NULL_DECL) return _sdecl;
    
    // Handle Class
    if(isClass())
        return _sdecl = _scn!=null? getJavaDecl(_scn) : null;
    
    // Get superclass and helper
    JavaDecl cdecl = getParent(), scdecl = cdecl!=null? cdecl.getSuper() : null;
    JavaDeclHpr schpr = scdecl!=null && scdecl.isClass()? scdecl.getHpr() : null;
    
    // Handle Method
    if(isMethod())
        return _sdecl = schpr!=null? schpr.getMethodDeclDeep(getName(), getArgTypes()) : null;
    
    // Handle Constructor
    if(isConstructor())
        return _sdecl = schpr!=null? schpr.getConstructorDeclDeep(getArgTypes()) : null;
        
    // Handle ParamType
    if(isParamType())
        return _sdecl = _par;
        
    // Complain and return
    System.err.println("JavaDecl.getSuper: Invalid type " + this);
    return _sdecl = null;
}

/**
 * Returns common ancestor of this decl and given decls.
 */
public JavaDecl getCommonAncestor(JavaDecl aDecl)
{
    if(aDecl==this) return this;
    
    // Handle primitive
    if(isPrimitive() && aDecl.isPrimitive())
        return getCommonAncestorPrimitive(aDecl);
    else if(isPrimitive())
        return getPrimitiveAlt().getCommonAncestor(aDecl);
    else if(aDecl.isPrimitive())
        return getCommonAncestor(aDecl.getPrimitiveAlt());
    
    // Iterate up each super chain to check
    for(JavaDecl d0=this;d0!=null;d0=d0.getSuper())
        for(JavaDecl d1=aDecl;d1!=null;d1=d1.getSuper())
            if(d0==d1) return d0;

    // Return Object (case where at least one was interface or ParamType of interface)
    return getJavaDecl(Object.class);
}

/**
 * Returns common ancestor of this decl and given decls.
 */
private JavaDecl getCommonAncestorPrimitive(JavaDecl aDecl)
{
    String n0 = getName(), n1 = aDecl.getName();
    if(n0.equals("double")) return this; if(n1.equals("double")) return aDecl;
    if(n0.equals("float")) return this; if(n1.equals("float")) return aDecl;
    if(n0.equals("long")) return this; if(n1.equals("long")) return aDecl;
    if(n0.equals("int")) return this; if(n1.equals("int")) return aDecl;
    if(n0.equals("short")) return this; if(n1.equals("short")) return aDecl;
    if(n0.equals("char")) return this; if(n1.equals("char")) return aDecl;
    return this;
}
    
/**
 * Returns the primitive counter part, if available.
 */
public JavaDecl getPrimitive()
{
    if(isPrimitive()) return this;
    switch(_name) {
        case "java.lang.Boolean": return getJavaDecl(boolean.class);
        case "java.lang.Byte": return getJavaDecl(byte.class);
        case "java.lang.Character": return getJavaDecl(char.class);
        case "java.lang.Short": return getJavaDecl(short.class);
        case "java.lang.Integer": return getJavaDecl(int.class);
        case "java.lang.Long": return getJavaDecl(long.class);
        case "java.lang.Float": return getJavaDecl(float.class);
        case "java.lang.Double": return getJavaDecl(double.class);
        default: return null;
    }
}

/**
 * Returns the primitive counter part, if available.
 */
public JavaDecl getPrimitiveAlt()
{
    if(!isPrimitive()) return this;
    switch(_name) {
        case "boolean": return getJavaDecl(Boolean.class);
        case "byte": return getJavaDecl(Byte.class);
        case "char": return getJavaDecl(Character.class);
        case "short": return getJavaDecl(Short.class);
        case "int": return getJavaDecl(Integer.class);
        case "long": return getJavaDecl(Long.class);
        case "float": return getJavaDecl(Float.class);
        case "double": return getJavaDecl(Double.class);
        default: return null;
    }
}

/**
 * Returns whether given type is assignable to this JavaDecl.
 */
public boolean isAssignable(JavaDecl aDecl)
{
    // If this decl is primitive, forward to primitive version
    if(isPrimitive()) return isAssignablePrimitive(aDecl);
    
    // If given val is null or this decl is Object return true
    if(aDecl==null) return true;
    JavaDecl ctype0 = getClassType(); if(ctype0.getName().equals("java.lang.Object")) return true;
    JavaDecl ctype1 = aDecl.getClassType(); if(ctype1.isPrimitive()) ctype1 = ctype1.getPrimitiveAlt();
    
    // Iterate up given class superclasses and check class and interfaces
    for(JavaDecl ct1=ctype1; ct1!=null; ct1=ct1.getSuper()) {
        if(ct1==ctype0)
            return true;
        if(ctype0.isInterface())
            for(JavaDecl infc : ct1.getHpr().getInterfaces())
                if(isAssignable(infc))
                    return true;
    }
    return false;
}

/**
 * Returns whether given type is assignable to this JavaDecl.
 */
private boolean isAssignablePrimitive(JavaDecl aDecl)
{
    if(aDecl==null) return false;
    JavaDecl ctype0 = getClassType();
    JavaDecl ctype1 = aDecl.getClassType().getPrimitive(); if(ctype1==null) return false;
    JavaDecl common = getCommonAncestorPrimitive(ctype1);
    return common==this;
}

/**
 * Returns a name suitable to describe declaration.
 */
public String getPrettyName()
{
    String name = getClassName();
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
    String name = getClassName();
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
        case Method: return getPrettyName().replace(getClassName() + '.', "");
        case Package: {
            String name = getPackageName(); int index = name.lastIndexOf('.');
            return index>0? name.substring(index+1) : name;
        }
        default: return getName();
    }
}

/**
 * Returns a JavaDecl for given object.
 */
public JavaDecl getJavaDecl(Object anObj)  { return _owner.getJavaDecl(anObj); }

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
        Class c1 = getParentClass(), c2 = aDecl.getParentClass();
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
public int hashCode()  { return getId().hashCode(); }

/**
 * Standard toString implementation.
 */
public String toString()  { return _type + ": " + getId(); }

}