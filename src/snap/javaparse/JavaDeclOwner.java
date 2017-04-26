package snap.javaparse;
import java.lang.reflect.*;
import java.util.*;
import snap.util.ClassUtils;

/**
 * A class that manages all the JavaDecls for a project.
 */
public abstract class JavaDeclOwner {

    // A map of JavaDecls objects to provide JavaDecls for project
    Map <String,JavaDecl>   _decls = new HashMap();
    
/**
 * Returns a JavaDecl for object.
 */
public JavaDecl getJavaDecl(Object anObj)
{
    // Handle String (class or package name)
    if(anObj instanceof String) { String name = (String)anObj;
        JavaDecl jd = _decls.get(name); if(jd!=null) return jd;
        
        // If class exists, create JavaDecl, add to Decls map and return
        ClassLoader cldr = getClassLoader();
        Class cls = ClassUtils.getClass(name, cldr);
        if(cls!=null)
            return createClassDecl(cls);
            
        // Since not found, just return
        //System.err.println("JavaDeclOwner.getJavaDecl: Unknown string decl reference: " + name);
        return null;
    }
    
    // Handle Class
    if(anObj instanceof Class) { Class cls = (Class)anObj; String name = cls.getName();
        JavaDecl jd = _decls.get(name);
        if(jd==null) _decls.put(name, jd = createClassDecl(cls));
        return jd;
    }
    
    // Do normal version
    return getJavaDecl(this, anObj);
}

/**
 * Creates a class decl.
 */
private JavaDecl createClassDecl(Class aClass)
{
    // Get parent decl
    JavaDecl parDecl = null;
    Class dcls = aClass.getDeclaringClass();
    if(dcls!=null)
        parDecl = getJavaDecl(dcls);
    else {
        Package pkg = aClass.getPackage();
        String pname = pkg!=null? pkg.getName() : null;
        if(pname!=null && pname.length()>0)
            parDecl = getPackageDecl(pname);
    }
    
    // Create and add JavaDecl for class
    JavaDecl decl = new JavaDecl(this, parDecl, aClass);
    _decls.put(aClass.getName(), decl);
    if(aClass.isArray())
        _decls.put(JavaDecl.getClassName(aClass), decl);
    return decl;
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
 * Returns a package decl.
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
 * Returns a JavaDecl for object.
 */
private static JavaDecl getJavaDecl(JavaDeclOwner aProj, Object anObj)
{
    // Handle Class
    JavaDecl jd = null;
    if(anObj instanceof Class) { Class cls = (Class)anObj, dcls = cls.getDeclaringClass();
        if(dcls==null)
            return aProj.getJavaDecl(cls);
        JavaDecl decl = aProj.getJavaDecl(dcls);
        JavaDeclHpr declHpr = decl.getHpr();
        jd = declHpr.getClassDecl(cls);
    }
    
    // Handle Field
    else if(anObj instanceof Field) { Field field = (Field)anObj; Class cls = field.getDeclaringClass();
        JavaDecl decl = aProj.getJavaDecl(cls);
        JavaDeclHpr declHpr = decl.getHpr();
        jd = declHpr.getFieldDecl(field);
    }
    
    // Handle Method
    else if(anObj instanceof Method) { Method meth = (Method)anObj; Class cls = meth.getDeclaringClass();
        JavaDecl decl = aProj.getJavaDecl(cls);
        JavaDeclHpr declHpr = decl.getHpr();
        jd = declHpr.getMethodDecl(meth);
    }

    // Handle Constructor
    else if(anObj instanceof Constructor) { Constructor constr = (Constructor)anObj; Class cls = constr.getDeclaringClass();
        JavaDecl decl = aProj.getJavaDecl(cls);
        JavaDeclHpr declHpr = decl.getHpr();
        jd = declHpr.getConstructorDecl(constr);
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
    else throw new RuntimeException("JavaDeclOwner.getJavaDecl: Unsupported type " + anObj);
    
    if(jd==null)
        System.out.println("JavaDecl.getJavaDecl: Decl not found for " + anObj);
    return jd;
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

}