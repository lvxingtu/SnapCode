package snap.javaparse;
import java.lang.reflect.*;
import java.util.*;
import snap.util.ArrayUtils;

/**
 * This class manages JavaDecls for a class.
 */
public class JavaDeclHpr {

    // The class decl this JavaDecls belongs to
    JavaDecl         _cdecl;
    
    // The super class decl helper
    JavaDeclHpr      _sdeclHpr;
    
    // The field decls
    List <JavaDecl>  _fdecls;

    // The method decls
    List <JavaDecl>  _mdecls = new ArrayList();

    // The constructor decls
    List <JavaDecl>  _cdecls = new ArrayList();

    // The inner class decls
    List <JavaDecl>  _icdecls = new ArrayList();
    
/**
 * Creates a new JavaDeclClass.
 */
public JavaDeclHpr(JavaDecl aCDecl)
{
    // Set project, class name and super decl
    _cdecl = aCDecl;
    Class cls = aCDecl.getEvalClass();
    Class scls = cls.getSuperclass();
    if(scls!=null)
        _sdeclHpr = aCDecl.getJavaDecl(scls.getName()).getHpr();
}

/**
 * Returns the class decl.
 */
public JavaDecl getClassDecl()  { return _cdecl; }

/**
 * Updates JavaDecls.
 */
public HashSet <JavaDecl> updateDecls()
{
    // If first time, set decls
    if(_fdecls==null) _fdecls = new ArrayList();
    
    // Get class
    Class cls = _cdecl.getEvalClass();
    String cname = _cdecl.getClassName();
    JavaDeclOwner owner = _cdecl._owner;
    
    // Create set for added/removed decls
    HashSet <JavaDecl> addedDecls = new HashSet();
    HashSet <JavaDecl> removedDecls = new HashSet(); if(_cdecl!=null) removedDecls.add(_cdecl);
    removedDecls.addAll(_fdecls); removedDecls.addAll(_mdecls);
    removedDecls.addAll(_cdecls); removedDecls.addAll(_icdecls);

    // Make sure class decl is up to date
    if(_cdecl.getModifiers()!=cls.getModifiers())
        _cdecl._mods = cls.getModifiers();
        
    // Inner Classes: Add JavaDecl for each inner class
    Class iclss[]; try { iclss = cls.getDeclaredClasses(); }
    catch(Throwable e) { System.err.println(e + " in " + cname); return null; }
    for(Class icls : iclss) {   //if(icls.isSynthetic()) continue;
        JavaDecl decl = getClassDecl(icls);
        if(decl==null) { decl = new JavaDecl(owner,_cdecl,icls); addedDecls.add(decl); addDecl(decl); }
        else removedDecls.remove(decl);
    }
    
    // TypeVariables: Add JavaDecl for each Type parameter
    Collections.addAll(removedDecls, _cdecl._typeVars);
    for(TypeVariable tv : cls.getTypeParameters()) { String name = tv.getName();
        JavaDecl decl = _cdecl.getTypeVar(name);
        if(decl==null) { decl = new JavaDecl(owner,_cdecl,tv); addedDecls.add(decl); addDecl(decl); }
        else removedDecls.remove(decl);
    }
    
    // Fields: add JavaDecl for each declared field - also make sure field type is in refs
    Field fields[]; try { fields = cls.getDeclaredFields(); }
    catch(Throwable e) { System.err.println(e + " in " + cname); return null; }
    for(Field field : fields) {
        JavaDecl decl = getFieldDecl(field);
        if(decl==null) { decl = new JavaDecl(owner,_cdecl,field); addedDecls.add(decl); addDecl(decl); }
        else removedDecls.remove(decl);
    }
    
    // Methods: Add JavaDecl for each declared method - also make sure return/parameter types are in refs
    Method methods[]; try { methods = cls.getDeclaredMethods(); }
    catch(Throwable e) { System.err.println(e + " in " + cname); return null; }
    for(Method meth : methods) {
        if(meth.isSynthetic()) continue;
        JavaDecl decl = getMethodDecl(meth);
        if(decl==null) { decl = new JavaDecl(owner,_cdecl,meth); addedDecls.add(decl); addDecl(decl); }
        else removedDecls.remove(decl);
    }
    
    // Constructors: Add JavaDecl for each constructor - also make sure parameter types are in refs
    Constructor constrs[]; try { constrs = cls.getDeclaredConstructors(); }
    catch(Throwable e) { System.err.println(e + " in " + cname); return null; }
    for(Constructor constr : constrs) {
        if(constr.isSynthetic()) continue;
        JavaDecl decl = getConstructorDecl(constr);
        if(decl==null) { decl = new JavaDecl(owner,_cdecl,constr); addedDecls.add(decl); addDecl(decl); }
        else removedDecls.remove(decl);
    }
    
    // Remove unused decls
    for(JavaDecl jd : removedDecls) removeDecl(jd);
    
    // Return all decls
    HashSet <JavaDecl> allDecls = new HashSet(); allDecls.add(_cdecl);
    allDecls.addAll(_fdecls); allDecls.addAll(_mdecls); allDecls.addAll(_cdecls); allDecls.addAll(_icdecls);
    return allDecls;
}

/**
 * Returns the field decl for field.
 */
public JavaDecl getFieldDecl(Field aField)
{
    String id = JavaDeclOwner.getId(aField);
    JavaDecl decl = getFieldDecl(id); if(decl==null) return null;
    int mods = aField.getModifiers(); if(mods!=decl.getModifiers()) return null;
    //JavaDecl type = _cdecl._owner.getTypeDecl(aField.getGenericType(), _cdecl);
    //if(type!=decl.getEvalType()) return null;
    return decl;
}

/**
 * Returns the field decl for id string.
 */
public JavaDecl getFieldDecl(String anId)
{
    if(_fdecls==null) updateDecls();
    for(JavaDecl jd : _fdecls) if(jd.getId().equals(anId)) return jd;
    return null;
}

/**
 * Returns a field decl for field name.
 */
public JavaDecl getFieldDecl(int theMods, String aName, JavaDecl aType)
{
    if(_fdecls==null) updateDecls();
    
    for(JavaDecl jd : _fdecls)
        if(jd.getName().equals(aName) && (aType==null || jd.getEvalType().equals(aType)) &&
            (theMods<0 || jd.getModifiers()==theMods))
                return jd;
    return null;
}

/**
 * Returns a field decl for field name.
 */
public JavaDecl getFieldDeclDeep(int theMods, String aName, JavaDecl aType)
{
    JavaDecl decl = getFieldDecl(theMods, aName, aType);
    if(decl==null && _sdeclHpr!=null) decl = _sdeclHpr.getFieldDeclDeep(theMods, aName, aType);
    return decl;
}

/**
 * Returns the method decl for method.
 */
public JavaDecl getMethodDecl(Method aMeth)
{
    String id = JavaDeclOwner.getId(aMeth);
    JavaDecl decl = getMethodDecl(id); if(decl==null) return null;
    int mods = aMeth.getModifiers(); if(mods!=decl.getModifiers()) return null;
    //java.lang.reflect.Type ptypes[] = aMeth.getGenericParameterTypes();
    //if(ptypes.length!=aMeth.getParameterCount()) ptypes = aMeth.getParameterTypes();
    //JavaDecl types[] = new JavaDecl[ptypes.length];
    //for(int i=0;i<types.length;i++) types[i] = getTypeDecl(ptypes[i]);
    return decl;
}

/**
 * Returns the method decl for id string.
 */
public JavaDecl getMethodDecl(String anId)
{
    if(_fdecls==null) updateDecls();
    for(JavaDecl jd : _mdecls) if(jd.getId().equals(anId)) return jd;
    return null;
}

/**
 * Returns a method decl for method mods, name and return/parameter type names.
 */
public JavaDecl getMethodDecl(int theMods, String aName, JavaDecl aType, JavaDecl theTypes[])
{
    if(_fdecls==null) updateDecls();
    
    for(JavaDecl jd : _mdecls)
        if(jd.getName().equals(aName) && (aType==null || jd.getEvalType().equals(aType)) &&
            Arrays.equals(jd.getArgTypes(), theTypes) && (theMods<0 || jd.getModifiers()==theMods))
                return jd;
    return null;
}

/**
 * Returns a method decl for method mods, name and return/parameter type names.
 */
public JavaDecl getMethodDeclDeep(int theMods, String aName, JavaDecl aType, JavaDecl theTypes[])
{
    JavaDecl decl = getMethodDecl(theMods, aName, aType, theTypes);
    if(decl==null && _sdeclHpr!=null) decl = _sdeclHpr.getMethodDeclDeep(theMods, aName, aType, theTypes);
    return decl;
}

/**
 * Returns the decl for constructor.
 */
public JavaDecl getConstructorDecl(Constructor aConstr)
{
    if(_fdecls==null) updateDecls();
    
    String id = JavaDeclOwner.getId(aConstr);
    JavaDecl decl = getConstructorDecl(id); if(decl==null) return null;
    int mods = aConstr.getModifiers(); if(mods!=decl.getModifiers()) return null;
    //java.lang.reflect.Type ptypes[] = aConstr.getGenericParameterTypes();
    //if(ptypes.length!=aConstr.getParameterCount()) ptypes = aConstr.getParameterTypes();
    //JavaDecl types[] = new JavaDecl[ptypes.length];
    //for(int i=0;i<types.length;i++) types[i] = getJavaDecl(ptypes[i]);
    return decl;
}

/**
 * Returns the Constructor decl for id string.
 */
public JavaDecl getConstructorDecl(String anId)
{
    if(_fdecls==null) updateDecls();
    for(JavaDecl jd : _cdecls) if(jd.getId().equals(anId)) return jd;
    return null;
}

/**
 * Returns a decl for constructor types.
 */
public JavaDecl getConstructorDecl(int theMods, JavaDecl theTypes[])
{
    for(JavaDecl jd : _cdecls)
        if(Arrays.equals(jd.getArgTypes(), theTypes) && (theMods<0 || jd.getModifiers()==theMods))
            return jd;
    return null;
}

/**
 * Returns a decl for constructor types.
 */
public JavaDecl getConstructorDeclDeep(int theMods, JavaDecl theTypes[])
{
    JavaDecl decl = getConstructorDecl(theMods, theTypes);
    if(decl==null && _sdeclHpr!=null) decl = _sdeclHpr.getConstructorDeclDeep(theMods, theTypes);
    return decl;
}

/**
 * Returns a Class decl for inner class.
 */
public JavaDecl getClassDecl(Class aClass)  { return getClassDecl(aClass.getName()); }

/**
 * Returns a Class decl for inner class name.
 */
public JavaDecl getClassDecl(String aName)
{
    if(_fdecls==null) updateDecls();
    
    for(JavaDecl jd : _icdecls)
        if(jd.getName().equals(aName))
                return jd;
    return null;
}

/**
 * Returns a Class decl for inner class name.
 */
public JavaDecl getClassDeclDeep(String aName)
{
    JavaDecl decl = getClassDecl(aName);
    if(decl==null && _sdeclHpr!=null) decl = _sdeclHpr.getClassDeclDeep(aName);
    return decl;
}

/**
 * Adds a TypeVar decl.
 */
private void addTypeVarDecl(JavaDecl aDecl)  { _cdecl._typeVars = ArrayUtils.add(_cdecl._typeVars,aDecl); }

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
        case TypeVar: addTypeVarDecl(aDecl); break;
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
        case TypeVar: _cdecl._typeVars = ArrayUtils.remove(_cdecl._typeVars, aDecl); break;
        default: throw new RuntimeException("JavaDeclHpr.removeDecl: Invalid type " + type);
    }
}

/**
 * Standard toString implementation.
 */
public String toString()  { return "ClassDecl { ClassName=" + _cdecl.getClassName() + " }"; }

/**
 * Returns a JavaDecl for object.
 */
public JavaDecl getJavaDecl(Object anObj)  { return _cdecl.getJavaDecl(anObj); }

}