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
    boolean        _enum, _interface;
    
    // The type this decl evaluates to when referenced
    JavaDecl       _evalType;
    
    // The JavaDecls for arg types for Constructor, Method
    JavaDecl       _argTypes[];
    
    // The JavaDecls for TypeVarss for Class, Method
    JavaDecl       _typeVars[] = EMPTY_DECLS;
    
    // The VariableDecl
    JVarDecl       _vdecl;
    
    // The JavaDeclHpr to access children of this class JavaDecl (fields, methods, constructors, inner classes)
    JavaDeclHpr    _hpr;
    
    // Constants for type
    public enum DeclType { Class, Field, Constructor, Method, Package, VarDecl, ParamType, TypeVar }
    
    // Shared empty TypeVar array
    private static JavaDecl[] EMPTY_DECLS = new JavaDecl[0];
    
/**
 * Creates a new JavaDecl for Class, Field, Constructor, Method, VarDecl or class name string.
 */
public JavaDecl(JavaDeclOwner anOwner, JavaDecl aPar, Object anObj)
{
    // Set JavaDecls
    _owner = anOwner; _par = aPar; assert(_owner!=null);
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
    
    // Handle Package String
    else if(anObj instanceof String) { String pname = (String)anObj; _type = DeclType.Package;
        _name = pname; _sname = JavaDeclOwner.getSimpleName(pname); }
    
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
        _typeVars = new JavaDecl[typArgs.length];
        for(int i=0,iMax=typArgs.length;i<iMax;i++) _typeVars[i] = _owner.getTypeDecl(typArgs[i], _par);
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
        _enum = cls.isEnum(); _interface = cls.isInterface();
        _evalType = this;
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
    else { Executable exec = (Executable)aMmbr;
    
        // Get TypeVars
        TypeVariable tvars[] = exec.getTypeParameters();
        _typeVars = new JavaDecl[tvars.length];
        for(int i=0,iMax=tvars.length;i<iMax;i++) _typeVars[i] = new JavaDecl(_owner,this,tvars[i]);
        
        // Get Return Type
        Type rtype = exec.getAnnotatedReturnType().getType();
        _evalType = _owner.getTypeDecl(rtype, this);
        
        // Handle Method
        if(exec instanceof Method) { Method meth = (Method)exec; _type = DeclType.Method; }
        //    _evalType = getJavaDecl(meth.getGenericReturnType()); }
            
        // Handle Constructor
        else if(exec instanceof Constructor) { Constructor constr = (Constructor)exec; _type = DeclType.Constructor; }
        //    _evalType = getJavaDecl(constr.getDeclaringClass()); }
        
        // Get GenericParameterTypes (this can fail https://bugs.openjdk.java.net/browse/JDK-8075483))
        Type ptypes[] = exec.getGenericParameterTypes();
        if(ptypes.length<exec.getParameterCount()) ptypes = exec.getParameterTypes();
        _argTypes = new JavaDecl[ptypes.length];
        for(int i=0,iMax=ptypes.length; i<iMax; i++)
            _argTypes[i] = _owner.getTypeDecl(ptypes[i], this);
    }
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
public boolean isArrayClass()  { return _type==DeclType.Class && _name.endsWith("[]"); }

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
 * Returns the class name.
 */
public String getClassName()
{
    if(isClass()) return getName();
    if(isTypeVar()) return getEvalClassName();
    return getParentClassName();
}

/**
 * Returns the class simple name.
 */
public String getClassSimpleName() { return isClass()? getSimpleName() : _par!=null? _par.getClassSimpleName() : null; }

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
 * Returns the TypeVars.
 */
public JavaDecl[] getTypeVars()  { return _typeVars; }

/**
 * Returns the TypeVar with given name.
 */
public JavaDecl getTypeVar(String aName)
{
    for(JavaDecl tv : _typeVars)
        if(tv.getName().equals(aName))
            return tv;
    if(_par!=null && _par.isClass())
        return _par.getTypeVar(aName);
    return null;
}

/**
 * Returns the type args (ParamType).
 */
public JavaDecl[] getTypeArgs()  { return _typeVars; }

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
public int hashCode()  { return getFullName().hashCode(); }

/**
 * Standard toString implementation.
 */
public String toString()  { return _type + ": " + getFullName(); }

}