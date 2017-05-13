package snap.javakit;
import java.lang.reflect.*;
import java.util.*;

/**
 * A subclass of JavaDecl especially for Class declarations.
 */
public class JavaDeclClass extends JavaDecl {

    // The super class decl
    JavaDeclClass    _scdecl;
    
    // Whether class decl is enum, interface, primitive
    boolean          _enum, _interface, _primitive;
    
    // The array of interfaces
    JavaDeclClass    _interfaces[];
    
    // The field decls
    List <JavaDecl>  _fdecls;

    // The method decls
    List <JavaDecl>  _mdecls = new ArrayList();

    // The constructor decls
    List <JavaDecl>  _cdecls = new ArrayList();

    // The inner class decls
    List <JavaDecl>  _icdecls = new ArrayList();
    
    // The type var decls
    List <JavaDecl>  _tvdecls = new ArrayList();
    
    // The Array item type (if Array)
    JavaDecl         _arrayItemType;
    
/**
 * Creates a new JavaDeclClass for given owner, parent and Class.
 */
public JavaDeclClass(JavaDeclOwner anOwner, JavaDecl aPar, Class aClass)
{
    // Do normal version
    super(anOwner, aPar, aClass);
    
    // Set class attributes
    _mods = aClass.getModifiers(); _type = DeclType.Class;
    _name = JavaKitUtils.getId(aClass); _sname = aClass.getSimpleName();
    _enum = aClass.isEnum(); _interface = aClass.isInterface(); _primitive = aClass.isPrimitive();
    _evalType = this; _sdecl = null; // Set by owner
    if(aClass.isArray())
        _arrayItemType = getJavaDecl(aClass.getComponentType());
}

/**
 * Returns whether is a class reference.
 */
public boolean isClass()  { return true; }

/**
 * Returns whether is a enum reference.
 */
public boolean isEnum()  { return _enum; }

/**
 * Returns whether is a interface reference.
 */
public boolean isInterface()  { return _interface; }

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
public boolean isPrimitive()  { return _primitive; }

/**
 * Returns the primitive counter part, if available.
 */
public JavaDeclClass getPrimitive()
{
    if(isPrimitive()) return this;
    switch(_name) {
        case "java.lang.Boolean": return getClassDecl(boolean.class);
        case "java.lang.Byte": return getClassDecl(byte.class);
        case "java.lang.Character": return getClassDecl(char.class);
        case "java.lang.Short": return getClassDecl(short.class);
        case "java.lang.Integer": return getClassDecl(int.class);
        case "java.lang.Long": return getClassDecl(long.class);
        case "java.lang.Float": return getClassDecl(float.class);
        case "java.lang.Double": return getClassDecl(double.class);
        default: return null;
    }
}

/**
 * Returns the primitive counter part, if available.
 */
public JavaDeclClass getPrimitiveAlt()
{
    if(!isPrimitive()) return this;
    switch(_name) {
        case "boolean": return getClassDecl(Boolean.class);
        case "byte": return getClassDecl(Byte.class);
        case "char": return getClassDecl(Character.class);
        case "short": return getClassDecl(Short.class);
        case "int": return getClassDecl(Integer.class);
        case "long": return getClassDecl(Long.class);
        case "float": return getClassDecl(Float.class);
        case "double": return getClassDecl(Double.class);
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
    if(getName().equals("java.lang.Object")) return true;
    JavaDeclClass ctype1 = aDecl.getClassType(); if(ctype1.isPrimitive()) ctype1 = ctype1.getPrimitiveAlt();
    
    // If both are array type, check ArrayItemTypes instead
    if(isArray() && ctype1.isArray())
        return getArrayItemType().isAssignable(ctype1.getArrayItemType());
    
    // Iterate up given class superclasses and check class and interfaces
    for(JavaDeclClass ct1=ctype1; ct1!=null; ct1=ct1.getSuper()) {
        
        // If classes match, return true
        if(ct1==this)
            return true;
            
        // If any interface of this decl match, return true
        if(isInterface())
            for(JavaDeclClass infc : ct1.getInterfaces())
                if(isAssignable(infc))
                    return true;
    }
    
    // Return false since no match found
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
 * Override to return as Class type.
 */
public JavaDeclClass getSuper()  { return _scdecl; }

/**
 * Updates JavaDecls.
 */
public HashSet <JavaDecl> updateDecls()
{
    // If first time, set decls
    if(_fdecls==null) _fdecls = new ArrayList();
    
    // Get class
    Class cls = getEvalClass();
    String cname = getClassName();
    
    // Get interfaces
    Class interfaces[] = cls.getInterfaces();
    _interfaces = new JavaDeclClass[interfaces.length];
    for(int i=0,iMax=interfaces.length;i<iMax;i++) { Class infc = interfaces[i];
        _interfaces[i] = getClassDecl(infc); }
    
    // Create set for added/removed decls
    HashSet <JavaDecl> addedDecls = new HashSet();
    HashSet <JavaDecl> removedDecls = new HashSet(); removedDecls.add(this);
    removedDecls.addAll(_fdecls); removedDecls.addAll(_mdecls);
    removedDecls.addAll(_cdecls); removedDecls.addAll(_icdecls);

    // Make sure class decl is up to date
    if(getModifiers()!=cls.getModifiers())
        _mods = cls.getModifiers();
        
    // Inner Classes: Add JavaDecl for each inner class
    Class iclss[]; try { iclss = cls.getDeclaredClasses(); }
    catch(Throwable e) { System.err.println(e + " in " + cname); return null; }
    for(Class icls : iclss) {   //if(icls.isSynthetic()) continue;
        JavaDecl decl = getClassDecl(icls.getSimpleName());
        if(decl==null) { decl = getJavaDecl(icls); addedDecls.add(decl); addDecl(decl); }
        else removedDecls.remove(decl);
    }
    
    // TypeVariables: Add JavaDecl for each Type parameter
    Collections.addAll(removedDecls, this._typeVars);
    for(TypeVariable tv : cls.getTypeParameters()) { String name = tv.getName();
        JavaDecl decl = getTypeVar(name);
        if(decl==null) { decl = new JavaDecl(_owner,this,tv); addedDecls.add(decl); addDecl(decl); }
        else removedDecls.remove(decl);
    }
    
    // Fields: add JavaDecl for each declared field - also make sure field type is in refs
    Field fields[]; try { fields = cls.getDeclaredFields(); }
    catch(Throwable e) { System.err.println(e + " in " + cname); return null; }
    for(Field field : fields) {
        JavaDecl decl = getField(field);
        if(decl==null) { decl = new JavaDecl(_owner,this,field); addedDecls.add(decl); addDecl(decl); }
        else removedDecls.remove(decl);
    }
    
    // Methods: Add JavaDecl for each declared method - also make sure return/parameter types are in refs
    Method methods[]; try { methods = cls.getDeclaredMethods(); }
    catch(Throwable e) { System.err.println(e + " in " + cname); return null; }
    for(Method meth : methods) {
        if(meth.isSynthetic()) continue;
        JavaDecl decl = getMethodDecl(meth);
        if(decl==null) { decl = new JavaDecl(_owner,this,meth); addedDecls.add(decl); addDecl(decl); }
        else removedDecls.remove(decl);
    }
    
    // Constructors: Add JavaDecl for each constructor - also make sure parameter types are in refs
    Constructor constrs[]; try { constrs = cls.getDeclaredConstructors(); }
    catch(Throwable e) { System.err.println(e + " in " + cname); return null; }
    for(Constructor constr : constrs) {
        if(constr.isSynthetic()) continue;
        JavaDecl decl = getConstructorDecl(constr);
        if(decl==null) { decl = new JavaDecl(_owner,this,constr); addedDecls.add(decl); addDecl(decl); }
        else removedDecls.remove(decl);
    }
    
    // Remove unused decls
    for(JavaDecl jd : removedDecls) removeDecl(jd);
    
    // Return all decls
    HashSet <JavaDecl> allDecls = new HashSet(); allDecls.add(this);
    allDecls.addAll(_fdecls); allDecls.addAll(_mdecls); allDecls.addAll(_cdecls); allDecls.addAll(_icdecls);
    return allDecls;
}

/**
 * Returns the interfaces this class implments.
 */
public JavaDeclClass[] getInterfaces()  { getFields(); return _interfaces; }

/**
 * Returns the fields.
 */
public List <JavaDecl> getFields()  { if(_fdecls==null) updateDecls(); return _fdecls; }

/**
 * Returns the methods.
 */
public List <JavaDecl> getMethods()  { getFields(); return _mdecls; }

/**
 * Returns the Constructors.
 */
public List <JavaDecl> getConstructors()  { getFields(); return _cdecls; }

/**
 * Returns the inner classes.
 */
public List <JavaDecl> getClasses()  { getFields(); return _icdecls; }

/**
 * Returns the inner classes.
 */
public List <JavaDecl> getTypeVars2()  { getFields(); return _tvdecls; }

/**
 * Returns the field decl for field.
 */
public JavaDecl getField(Field aField)
{
    String name = aField.getName();
    JavaDecl decl = getField(name); if(decl==null) return null;
    int mods = aField.getModifiers(); if(mods!=decl.getModifiers()) return null;
    //JavaDecl type = _cdecl._owner.getTypeDecl(aField.getGenericType(), _cdecl);
    //if(type!=decl.getEvalType()) return null;
    return decl;
}

/**
 * Returns a field decl for field name.
 */
public JavaDecl getField(String aName)
{
    List <JavaDecl> fdecls = getFields();
    for(JavaDecl jd : fdecls) if(jd.getName().equals(aName)) return jd;
    return null;
}

/**
 * Returns a field decl for field name.
 */
public JavaDecl getFieldDeep(String aName)
{
    JavaDecl decl = getField(aName);
    if(decl==null && _scdecl!=null) decl = _scdecl.getFieldDeep(aName);
    return decl;
}

/**
 * Returns the method decl for method.
 */
public JavaDecl getMethodDecl(Method aMeth)
{
    String id = JavaKitUtils.getId(aMeth);
    JavaDecl decl = getMethodDecl(id); if(decl==null) return null;
    int mods = aMeth.getModifiers(); if(mods!=decl.getModifiers()) return null;
    // Check return type?
    return decl;
}

/**
 * Returns the method decl for id string.
 */
public JavaDecl getMethodDecl(String anId)
{
    List <JavaDecl> mdecls = getMethods();
    for(JavaDecl jd : mdecls) if(jd.getId().equals(anId)) return jd;
    return null;
}

/**
 * Returns a method decl for method name and parameter types.
 */
public JavaDecl getMethodDecl(String aName, JavaDecl theTypes[])
{
    List <JavaDecl> mdecls = getMethods();
    for(JavaDecl jd : mdecls)
        if(jd.getName().equals(aName) && isClassTypesEqual(jd.getParamTypes(), theTypes))
            return jd;
    return null;
}

/**
 * Returns a method decl for method name and parameter types.
 */
public JavaDecl getMethodDeclDeep(String aName, JavaDecl theTypes[])
{
    JavaDecl decl = getMethodDecl(aName, theTypes);
    if(decl==null && _scdecl!=null) decl = _scdecl.getMethodDeclDeep(aName, theTypes);
    return decl;
}

/**
 * Returns a compatibile method for given name and param types.
 */
public JavaDecl getCompatibleConstructor(JavaDecl theTypes[])
{
    List <JavaDecl> cdecls = getConstructors();
    JavaDecl constr = null; int rating = 0;
    for(JavaDecl cd : cdecls) {
        int rtg = getMethodRating(cd, theTypes);
        if(rtg>rating) { constr = cd; rating = rtg; }
    }
    return constr;
}

/**
 * Returns a compatibile method for given name and param types.
 */
public JavaDecl getCompatibleMethod(String aName, JavaDecl theTypes[])
{
    List <JavaDecl> mdecls = getMethods();
    JavaDecl meth = null; int rating = 0;
    for(JavaDecl md : mdecls)
        if(md.getName().equals(aName)) {
            int rtg = getMethodRating(md, theTypes);
            if(rtg>rating) { meth = md; rating = rtg; }
        }
    return meth;
}

/**
 * Returns a compatibile method for given name and param types.
 */
public JavaDecl getCompatibleMethodDeep(String aName, JavaDecl theTypes[])
{
    // Search this class and superclasses for compatible method
    for(JavaDeclClass cls=this;cls!=null;cls=cls.getSuper()) {
        JavaDecl decl = cls.getCompatibleMethod(aName, theTypes);
        if(decl!=null)
            return decl;
    }
    return null;
}

/**
 * Returns a compatibile method for given name and param types.
 */
public JavaDecl getCompatibleMethodAll(String aName, JavaDecl theTypes[])
{
    // Search this class and superclasses for compatible method
    JavaDecl decl = getCompatibleMethodDeep(aName, theTypes);
    if(decl!=null)
        return decl;
    
    // Search this class and superclasses for compatible interface
    for(JavaDeclClass cls=this;cls!=null;cls=cls.getSuper()) {
        for(JavaDeclClass infc : cls.getInterfaces()) {
            decl = infc.getCompatibleMethodAll(aName, theTypes);
            if(decl!=null)
                return decl;
        }
    }
    
    // If this class is Interface, check Object
    if(isInterface()) {
        JavaDeclClass objDecl = getClassDecl(Object.class);
        return objDecl.getCompatibleMethodDeep(aName, theTypes);
    }
    
    // Return null since compatible method not found
    return null;
}

/**
 * Returns a compatibile method for given name and param types.
 */
public List <JavaDecl> getCompatibleMethods(String aName, JavaDecl theTypes[])
{
    List <JavaDecl> matches = Collections.EMPTY_LIST;
    List <JavaDecl> mdecls = getMethods();
    for(JavaDecl md : mdecls)
        if(md.getName().equals(aName)) {
            int rtg = getMethodRating(md, theTypes);
            if(rtg>0) {
                if(matches==Collections.EMPTY_LIST) matches = new ArrayList(); matches.add(md); }
        }
    return matches;
}

/**
 * Returns a compatibile method for given name and param types.
 */
public List <JavaDecl> getCompatibleMethodsDeep(String aName, JavaDecl theTypes[])
{
    // Search this class and superclasses for compatible method
    List <JavaDecl> matches = Collections.EMPTY_LIST;
    for(JavaDeclClass cls=this;cls!=null;cls=cls.getSuper()) {
        List <JavaDecl> decls = cls.getCompatibleMethods(aName, theTypes);
        if(decls.size()>0) {
            if(matches==Collections.EMPTY_LIST) matches = decls; else matches.addAll(decls); }
    }
    return matches;
}

/**
 * Returns a compatibile method for given name and param types.
 */
public List <JavaDecl> getCompatibleMethodsAll(String aName, JavaDecl theTypes[])
{
    // Search this class and superclasses for compatible method
    List <JavaDecl> matches = Collections.EMPTY_LIST;
    List <JavaDecl> decls = getCompatibleMethodsDeep(aName, theTypes);
    if(decls.size()>0) {
        if(matches==Collections.EMPTY_LIST) matches = decls; else matches.addAll(decls); }
    
    // Search this class and superclasses for compatible interface
    for(JavaDeclClass cls=this;cls!=null;cls=cls.getSuper()) {
        for(JavaDeclClass infc : cls.getInterfaces()) {
            decls = infc.getCompatibleMethodsAll(aName, theTypes);
            if(decls.size()>0) {
                if(matches==Collections.EMPTY_LIST) matches = decls; else matches.addAll(decls); }
        }
    }
    
    // If this class is Interface, check Object
    if(isInterface()) {
        JavaDeclClass objDecl = getClassDecl(Object.class);
        decls = objDecl.getCompatibleMethodsDeep(aName, theTypes);
        if(decls.size()>0) {
            if(matches==Collections.EMPTY_LIST) matches = decls; else matches.addAll(decls); }
    }
    
    // Remove supers and duplicates
    for(int i=0;i<matches.size();i++) { JavaDecl decl = matches.get(i);
        for(JavaDecl sd=decl.getSuper();sd!=null;sd=sd.getSuper()) matches.remove(sd);
        for(int j=i+1;j<matches.size();j++) if(matches.get(j)==decl) matches.remove(j);
    }
    
    // Return null since compatible method not found
    return matches;
}

/**
 * Returns whether decl class types are equal.
 */
public boolean isClassTypesEqual(JavaDecl theTypes0[], JavaDecl theTypes1[])
{
    int len = theTypes0.length; if(theTypes1.length!=len) return false;
    for(int i=0;i<len;i++) {
        JavaDecl ct0 = theTypes0[i]; if(ct0!=null) ct0 = ct0.getClassType();
        JavaDecl ct1 = theTypes1[i]; if(ct1!=null) ct1 = ct1.getClassType();
        if(ct0!=ct1)
            return false;
    }
    return true;
}

/**
 * Returns a rating of a method for given possible arg classes.
 */
private int getMethodRating(JavaDecl aMeth, JavaDecl theTypes[])
{
    // Handle VarArg methods special
    if(aMeth.isVarArgs()) return getMethodRatingVarArgs(aMeth, theTypes);
    
    // Get method param types and length (just return if given arg count doesn't match)
    JavaDecl paramTypes[] = aMeth.getParamTypes(); int plen = paramTypes.length, rating = 0;
    if(theTypes.length!=plen)
        return 0;
    if(plen==0)
        return 1000;

    // Iterate over classes and add score based on matching classes
    // This is a punt - need to groc the docs on this: https://docs.oracle.com/javase/specs/jls/se7/html/jls-15.html
    for(int i=0, iMax=plen; i<iMax; i++) {
        JavaDecl cls1 = paramTypes[i].getClassType(), cls2 = theTypes[i]; if(cls2!=null) cls2 = cls2.getClassType();
        if(!cls1.isAssignable(cls2))
            return 0;
        rating += cls1==cls2? 1000 : cls2!=null? 100 : 10;
    }
    
    // Return rating
    return rating;
}

/**
 * Returns a rating of a method for given possible arg classes.
 */
private int getMethodRatingVarArgs(JavaDecl aMeth, JavaDecl theTypes[])
{
    // Get method param types and length (just return if given arg count is insufficient)
    JavaDecl paramTypes[] = aMeth.getParamTypes(); int plen = paramTypes.length, vind = plen -1, rating = 0;
    if(theTypes.length<vind)
        return 0;
    if(plen==1 && theTypes.length==0)
        return 10;

    // Iterate over classes and add score based on matching classes
    // This is a punt - need to groc the docs on this: https://docs.oracle.com/javase/specs/jls/se7/html/jls-15.html
    for(int i=0, iMax=vind; i<iMax; i++) {
        JavaDecl cls1 = paramTypes[i].getClassType(), cls2 = theTypes[i]; if(cls2!=null) cls2 = cls2.getClassType();
        if(!cls1.isAssignable(cls2))
            return 0;
        rating += cls1==cls2? 1000 : cls2!=null? 100 : 10;
    }
    
    // Get VarArg type
    JavaDecl varArgArrayType = paramTypes[vind];
    JavaDecl varArgType = varArgArrayType.getArrayItemType();
    
    // If only one arg and it is of array type, add 1000
    JavaDecl argType = theTypes.length==plen? theTypes[vind] : null;
    if(argType!=null && argType.isArray() && varArgArrayType.isAssignable(argType))
        rating += 1000;

    // If any var args match, add 1000
    else for(int i=vind; i<theTypes.length; i++) { JavaDecl type = theTypes[i];
        if(varArgType.isAssignable(type))
            rating += 1000; }
    
    // Return rating
    return rating;
}

/**
 * Returns the lambda method.
 */
public JavaDecl getLambdaMethod(int argCount)
{
    List <JavaDecl> mdecls = getMethods();
    for(JavaDecl jd : mdecls)
        if(jd.getParamCount()==argCount)
            return jd;
    return null;
}

/**
 * Returns the decl for constructor.
 */
public JavaDecl getConstructorDecl(Constructor aConstr)
{
    String id = JavaKitUtils.getId(aConstr);
    JavaDecl decl = getConstructorDecl(id); if(decl==null) return null;
    int mods = aConstr.getModifiers(); if(mods!=decl.getModifiers()) return null;
    return decl;
}

/**
 * Returns the Constructor decl for id string.
 */
public JavaDecl getConstructorDecl(String anId)
{
    List <JavaDecl> cdecls = getConstructors();
    for(JavaDecl jd : cdecls) if(jd.getId().equals(anId)) return jd;
    return null;
}

/**
 * Returns a constructor decl for parameter types.
 */
public JavaDecl getConstructorDecl(JavaDecl theTypes[])
{
    List <JavaDecl> cdecls = getConstructors();
    for(JavaDecl jd : cdecls)
        if(isClassTypesEqual(jd.getParamTypes(), theTypes))
            return jd;
    return null;
}

/**
 * Returns a constructor decl for parameter types.
 */
public JavaDecl getConstructorDeclDeep(JavaDecl theTypes[])
{
    JavaDecl decl = getConstructorDecl(theTypes);
    if(decl==null && _scdecl!=null) decl = _scdecl.getConstructorDeclDeep(theTypes);
    return decl;
}

/**
 * Returns a Class decl for inner class simple name.
 */
public JavaDecl getClassDecl(String aName)
{
    List <JavaDecl> icdecls = getClasses();
    for(JavaDecl jd : icdecls)
        if(jd.getSimpleName().equals(aName))
                return jd;
    return null;
}

/**
 * Returns a Class decl for inner class name.
 */
public JavaDecl getClassDeclDeep(String aName)
{
    JavaDecl decl = getClassDecl(aName);
    if(decl==null && _scdecl!=null) decl = _scdecl.getClassDeclDeep(aName);
    return decl;
}

/**
 * Returns a TypeVar decl for inner class name.
 */
public JavaDecl getTypeVar(String aName)
{
    List <JavaDecl> tvdecls = getTypeVars2();
    for(JavaDecl jd : tvdecls)
        if(jd.getName().equals(aName))
                return jd;
    return null;
}

/**
 * Returns a TypeVar decl for inner class name.
 */
public int getTypeVarIndex(String aName)
{
    List <JavaDecl> tvdecls = getTypeVars2();
    for(int i=0,iMax=tvdecls.size();i<iMax;i++) { JavaDecl jd = tvdecls.get(i);
        if(jd.getName().equals(aName))
                return i; }
    return -1;
}

/**
 * Adds a decl.
 */
public void addDecl(JavaDecl aDecl)
{
    JavaDecl.DeclType type = aDecl.getType();
    switch(type) {
        case Field: _fdecls.add(aDecl); break;
        case Method: _mdecls.add(aDecl); break;
        case Constructor: _cdecls.add(aDecl); break;
        case Class: _icdecls.add(aDecl); break;
        case TypeVar: _tvdecls.add(aDecl); break;
        default: throw new RuntimeException("JavaDeclHpr.addDecl: Invalid type " + type);
    }
}

/**
 * Removes a decl.
 */
public void removeDecl(JavaDecl aDecl)
{
    JavaDecl.DeclType type = aDecl.getType();
    switch(type) {
        case Field: _fdecls.remove(aDecl); break;
        case Method: _mdecls.remove(aDecl); break;
        case Constructor: _cdecls.remove(aDecl); break;
        case Class: _icdecls.remove(aDecl); break;
        case TypeVar: _tvdecls.remove(aDecl); break;
        default: throw new RuntimeException("JavaDeclHpr.removeDecl: Invalid type " + type);
    }
}

/**
 * Standard toString implementation.
 */
public String toString()  { return "ClassDecl { ClassName=" + getClassName() + " }"; }

}