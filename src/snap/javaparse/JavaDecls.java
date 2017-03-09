package snap.javaparse;
import java.lang.reflect.*;
import java.util.*;
import snap.project.Project;

/**
 * This class manages JavaDecls for a class.
 */
public class JavaDecls {

    // The project this class works for
    Project          _proj;
    
    // The class name
    String           _cname;
    
    // The class decl
    JavaDecl         _cdecl;
    
    // The super class decl
    JavaDecls        _sdecl;
    
    // The field decls
    List <JavaDecl>  _fdecls = new ArrayList();

    // The method decls
    List <JavaDecl>  _mdecls = new ArrayList();

    // The constructor decls
    List <JavaDecl>  _cdecls = new ArrayList();

    // Map of package name to JavaDecls
    static Map <String,JavaDecl>  _pkgDecls = new HashMap();
    
/**
 * Creates a new JavaDecls.
 */
public JavaDecls(Project aProj, String aClassName)
{
    // Set project, class name and super decl
    _proj = aProj; _cname = aClassName;
    Class cls = aProj.getClassForName(_cname);
    Class scls = cls.getSuperclass();
    if(scls!=null)
        _sdecl = aProj.getJavaDecls(scls.getName());
        
    // Set base classes from JavaDecl constants
    if(cls==int.class) _cdecl = JavaDecl.INT_DECL;
    else if(cls==boolean.class) _cdecl = JavaDecl.BOOL_DECL;
    else if(cls==Object.class) _cdecl = JavaDecl.OBJECT_DECL;
    else if(cls==Class.class) _cdecl = JavaDecl.CLASS_DECL;
    else if(cls==String.class) _cdecl = JavaDecl.STRING_DECL;
    
    // Do initial load
    updateDecls();
}

/**
 * Returns the project.
 */
public Project getProject()  { return _proj; }

/**
 * Returns the super class decl.
 */
public JavaDecls getSuperClassDecl()  { return _sdecl; }

/**
 * Updates JavaDecls.
 */
public HashSet <JavaDecl> updateDecls()
{
    // Get class
    Class cls = _proj.getClassForName(_cname);
    
    // Create set for added/removed decls
    HashSet <JavaDecl> addedDecls = new HashSet();
    HashSet <JavaDecl> removedDecls = new HashSet(); if(_cdecl!=null) removedDecls.add(_cdecl);
    removedDecls.addAll(_fdecls); removedDecls.addAll(_mdecls); removedDecls.addAll(_cdecls);

    // Get class and make sure TypeParameters, superclass and interfaces are in refs
    if(_cdecl==null || _cdecl.getModifiers()!=cls.getModifiers()) {
        JavaDecl decl = _cdecl = new JavaDecl(this,cls); addedDecls.add(decl); }
    else removedDecls.remove(_cdecl);
    
    // Fields: add JavaDecl for each declared field - also make sure field type is in refs
    Field fields[]; try { fields = cls.getDeclaredFields(); }
    catch(Throwable e) { System.err.println(e + " in " + _cname); return null; }
    for(Field field : fields) {
        JavaDecl decl = getFieldDecl(field);
        if(decl==null) { decl = new JavaDecl(this,field); addedDecls.add(decl); _fdecls.add(decl); }
        else removedDecls.remove(decl);
    }
    
    // Methods: Add JavaDecl for each declared method - also make sure return/parameter types are in refs
    Method methods[]; try { methods = cls.getDeclaredMethods(); }
    catch(Throwable e) { System.err.println(e + " in " + _cname); return null; }
    for(Method meth : methods) {
        if(meth.isSynthetic()) continue;
        JavaDecl decl = getMethodDecl(meth);
        if(decl==null) { decl = new JavaDecl(this,meth); addedDecls.add(decl); _mdecls.add(decl); }
        else removedDecls.remove(decl);
    }
    
    // Constructors: Add JavaDecl for each constructor - also make sure parameter types are in refs
    Constructor constrs[]; try { constrs = cls.getDeclaredConstructors(); }
    catch(Throwable e) { System.err.println(e + " in " + _cname); return null; }
    for(Constructor constr : constrs) {
        if(constr.isSynthetic()) continue;
        JavaDecl decl = getConstructorDecl(constr);
        if(decl==null) { decl = new JavaDecl(this,constr); addedDecls.add(decl); _cdecls.add(decl); }
        else removedDecls.remove(decl);
    }
    
    // Remove unused decls
    for(JavaDecl jd : removedDecls) removeDecl(jd);
    
    // Return all decls
    HashSet <JavaDecl> allDecls = new HashSet(); allDecls.add(_cdecl);
    allDecls.addAll(_fdecls); allDecls.addAll(_mdecls); allDecls.addAll(_cdecls);
    return allDecls;
}

/**
 * Returns the class decl.
 */
public JavaDecl getClassDecl()  { return _cdecl; }

/**
 * Returns the field decl for field.
 */
public JavaDecl getFieldDecl(Field aField)
{
    int mods = aField.getModifiers();
    String name = aField.getName();
    String type = JavaDecl.getTypeName(aField.getGenericType());
    return getFieldDecl(mods, name, type);
}

/**
 * Returns a field decl for field name.
 */
public JavaDecl getFieldDecl(int theMods, String aName, String aType)
{
    for(JavaDecl jd : _fdecls)
        if(jd.getName().equals(aName) && (aType==null || jd.getTypeName().equals(aType)) &&
            (theMods<0 || jd.getModifiers()==theMods))
                return jd;
    return null;
}

/**
 * Returns a field decl for field name.
 */
public JavaDecl getFieldDeclDeep(int theMods, String aName, String aType)
{
    JavaDecl decl = getFieldDecl(theMods, aName, aType);
    if(decl==null && _sdecl!=null) decl = _sdecl.getFieldDeclDeep(theMods, aName, aType);
    return decl;
}

/**
 * Returns the method decl for method.
 */
public JavaDecl getMethodDecl(Method aMeth)
{
    int mods = aMeth.getModifiers();
    String name = aMeth.getName();
    String type = JavaDecl.getTypeName(aMeth.getGenericReturnType());
    java.lang.reflect.Type ptypes[] = aMeth.getGenericParameterTypes();
    String types[] = new String[ptypes.length];
    for(int i=0;i<types.length;i++) types[i] = JavaDecl.getTypeName(ptypes[i]);
    return getMethodDecl(mods, name, type, types);
}

/**
 * Returns a method decl for method mods, name and return/parameter type names.
 */
public JavaDecl getMethodDecl(int theMods, String aName, String aType, String theTypes[])
{
    for(JavaDecl jd : _mdecls)
        if(jd.getName().equals(aName) && (aType==null || jd.getTypeName().equals(aType)) &&
            Arrays.equals(jd._argTypeNames, theTypes) && (theMods<0 || jd.getModifiers()==theMods))
                return jd;
    return null;
}

/**
 * Returns a method decl for method mods, name and return/parameter type names.
 */
public JavaDecl getMethodDeclDeep(int theMods, String aName, String aType, String theTypes[])
{
    JavaDecl decl = getMethodDecl(theMods, aName, aType, theTypes);
    if(decl==null && _sdecl!=null) decl = _sdecl.getMethodDeclDeep(theMods, aName, aType, theTypes);
    return decl;
}

/**
 * Returns the decl for constructor.
 */
public JavaDecl getConstructorDecl(Constructor aConstr)
{
    int mods = aConstr.getModifiers();
    java.lang.reflect.Type ptypes[] = aConstr.getGenericParameterTypes();
    if(ptypes.length!=aConstr.getParameterCount()) ptypes = aConstr.getParameterTypes();
    String types[] = new String[ptypes.length];
    for(int i=0;i<types.length;i++) types[i] = JavaDecl.getTypeName(ptypes[i]);
    return getConstructorDecl(mods, types);
}

/**
 * Returns a decl for constructor types.
 */
public JavaDecl getConstructorDecl(int theMods, String theTypes[])
{
    for(JavaDecl jd : _cdecls)
        if(Arrays.equals(jd._argTypeNames, theTypes) && (theMods<0 || jd.getModifiers()==theMods))
            return jd;
    return null;
}

/**
 * Returns a decl for constructor types.
 */
public JavaDecl getConstructorDeclDeep(int theMods, String theTypes[])
{
    JavaDecl decl = getConstructorDecl(theMods, theTypes);
    if(decl==null && _sdecl!=null) decl = _sdecl.getConstructorDeclDeep(theMods, theTypes);
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
}

/**
 * Standard toString implementation.
 */
public String toString()  { return "ClassDecl { ClassName=" + _cname + " }"; }

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
 * Returns a JavaDecl for object.
 */
public static JavaDecl getJavaDecl(Project aProj, Object anObj)
{
    // Handle Class
    if(anObj instanceof Class) { Class cls = (Class)anObj;
        JavaDecls decls = aProj.getJavaDecls(cls.getName());
        return decls.getClassDecl();
    }
    
    // Handle Field
    if(anObj instanceof Field) { Field field = (Field)anObj; Class cls = field.getDeclaringClass();
        JavaDecls decls = aProj.getJavaDecls(cls.getName());
        return decls.getFieldDecl(field);
    }
    
    // Handle Method
    if(anObj instanceof Method) { Method meth = (Method)anObj; Class cls = meth.getDeclaringClass();
        JavaDecls decls = aProj.getJavaDecls(cls.getName());
        return decls.getMethodDecl(meth);
    }

    // Handle Constructor
    if(anObj instanceof Constructor) { Constructor constr = (Constructor)anObj; Class cls = constr.getDeclaringClass();
        JavaDecls decls = aProj.getJavaDecls(cls.getName());
        return decls.getConstructorDecl(constr);
    }
    
    // Handle JVarDecl
    if(anObj instanceof JVarDecl)  { JVarDecl vd = (JVarDecl)anObj;
        JClassDecl cd = vd.getParent(JClassDecl.class);
        JavaDecls decls = aProj.getJavaDecls(cd.getClassName());
        return new JavaDecl(decls, vd);
    }
    
    // Handle string
    if(anObj instanceof String) { String cname = (String)anObj; Class cls = aProj.getClassForName(cname);
        JavaDecls decls = cls!=null? aProj.getJavaDecls(cname) : null;
        return decls!=null? decls.getClassDecl() : null;
    }

    // Complain
    throw new RuntimeException("Project.getJavaDecl: Unsupported type " + anObj);
}

/**
 * Returns the JavaDecl for given package name from shared cache.
 */
public static JavaDecl getPackageDecl(String aName)
{
    JavaDecl pd = _pkgDecls.get(aName); if(pd!=null) return pd;
    pd = new JavaDecl(null,aName); pd._type = JavaDecl.DeclType.Package;
    pd._name = pd._pname = aName; pd._sname = JavaDecl.getSimpleName(aName);
    _pkgDecls.put(aName, pd);
    return pd;
}

}