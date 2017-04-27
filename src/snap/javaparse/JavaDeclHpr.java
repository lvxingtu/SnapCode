package snap.javaparse;
import java.lang.reflect.*;
import java.util.*;

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
        if(decl==null) { decl = new JavaDecl(owner,_cdecl,icls); addedDecls.add(decl); _icdecls.add(decl); }
        else removedDecls.remove(decl);
    }
    
    // Fields: add JavaDecl for each declared field - also make sure field type is in refs
    Field fields[]; try { fields = cls.getDeclaredFields(); }
    catch(Throwable e) { System.err.println(e + " in " + cname); return null; }
    for(Field field : fields) {
        JavaDecl decl = getFieldDecl(field);
        if(decl==null) { decl = new JavaDecl(owner,_cdecl,field); addedDecls.add(decl); _fdecls.add(decl); }
        else removedDecls.remove(decl);
    }
    
    // Methods: Add JavaDecl for each declared method - also make sure return/parameter types are in refs
    Method methods[]; try { methods = cls.getDeclaredMethods(); }
    catch(Throwable e) { System.err.println(e + " in " + cname); return null; }
    for(Method meth : methods) {
        if(meth.isSynthetic()) continue;
        JavaDecl decl = getMethodDecl(meth);
        if(decl==null) { decl = new JavaDecl(owner,_cdecl,meth); addedDecls.add(decl); _mdecls.add(decl); }
        else removedDecls.remove(decl);
    }
    
    // Constructors: Add JavaDecl for each constructor - also make sure parameter types are in refs
    Constructor constrs[]; try { constrs = cls.getDeclaredConstructors(); }
    catch(Throwable e) { System.err.println(e + " in " + cname); return null; }
    for(Constructor constr : constrs) {
        if(constr.isSynthetic()) continue;
        JavaDecl decl = getConstructorDecl(constr);
        if(decl==null) { decl = new JavaDecl(owner,_cdecl,constr); addedDecls.add(decl); _cdecls.add(decl); }
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
    int mods = aField.getModifiers();
    String name = aField.getName();
    JavaDecl type = getJavaDecl(aField.getGenericType());
    return getFieldDecl(mods, name, type);
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
    int mods = aMeth.getModifiers();
    String name = aMeth.getName();
    JavaDecl type = getJavaDecl(aMeth.getGenericReturnType());
    java.lang.reflect.Type ptypes[] = aMeth.getGenericParameterTypes();
    JavaDecl types[] = new JavaDecl[ptypes.length];
    for(int i=0;i<types.length;i++) types[i] = getJavaDecl(ptypes[i]);
    return getMethodDecl(mods, name, type, types);
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
    
    int mods = aConstr.getModifiers();
    java.lang.reflect.Type ptypes[] = aConstr.getGenericParameterTypes();
    if(ptypes.length!=aConstr.getParameterCount()) ptypes = aConstr.getParameterTypes();
    JavaDecl types[] = new JavaDecl[ptypes.length];
    for(int i=0;i<types.length;i++) types[i] = getJavaDecl(ptypes[i]);
    return getConstructorDecl(mods, types);
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
 * Returns a Class decl for inner class name.
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
 * Removes a decl.
 */
public void removeDecl(JavaDecl aDecl)
{
    if(aDecl.isField()) _fdecls.remove(aDecl);
    else if(aDecl.isMethod()) _mdecls.remove(aDecl);
    else if(aDecl.isConstructor()) _cdecls.remove(aDecl);
    else if(aDecl.isClass()) _icdecls.remove(aDecl);
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