/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snap.javaparse;
import java.lang.reflect.*;
import java.util.*;
import snap.project.*;
import snap.util.*;
import snap.web.*;

/**
 * A class to represent a completion suggestion for a given prefix.
 */
public class JavaSuggestion {

/**
 * Returns completion for JNode (should be JType or JIdentifier).
 */
public static JavaDecl[] getSuggestions(JNode aNode)
{
    // Create list
    List <JavaDecl> list = new ArrayList();

    // Add suggestions for node
    if(aNode instanceof JType)
        getSuggestions((JType)aNode, list);
    else if(aNode instanceof JExprId)
        getSuggestions((JExprId)aNode, list);
    
    // Sort list and return
    Project proj = Project.get(aNode.getFile().getSourceFile());
    Class reccls = getReceivingClass(aNode);
    JavaDecl decls[] = list.toArray(new JavaDecl[0]);
    Arrays.sort(decls, new DeclCompare(proj, reccls));
    return decls;
}

/**
 * Override to add suggestions for JavaType.
 */
private static void getSuggestions(JType aJType, List <JavaDecl> theSuggestions)
{
    ClassPathInfo cpinfo = getClassPathInfo(aJType);
    Project proj = cpinfo.getProject();
    String name = aJType.getName();
    if(name!=null) {
        List <String> cnames = cpinfo.getAllClassNames(name);
        for(String cname : cnames) {
            if(aJType.getParent() instanceof JExprAlloc) {
                Class cls = ClassUtils.getClass(cname, proj.getClassLoader());
                if(cls==null || !Modifier.isPublic(cls.getModifiers())) continue;
                Constructor cstrs[] = null; try { cstrs = cls.getConstructors(); } catch(Throwable t) { }
                if(cstrs!=null) for(Constructor cstr : cstrs)
                    theSuggestions.add(new JavaDecl(cstr));
            }
            else theSuggestions.add(new JavaDecl(cname, null, null, null));
        }
    }
}

/**
 * Override to add suggestions for JIdentifier.
 */
private static void getSuggestions(JExprId anId, List <JavaDecl> theSuggestions)
{
    // Get prefix string
    String prefix = anId.getName();
    ClassPathInfo cpinfo = getClassPathInfo(anId);
    Project proj = cpinfo.getProject();
    
    // If there is a parent expression, work from it
    JExpr parExpr = anId.getParentExpr();
    if(parExpr!=null) {
        
        // Handle parent is Package: Add packages and classes with prefix
        if(parExpr instanceof JExprId && ((JExprId)parExpr).isPackageName()) { JExprId parId = (JExprId)parExpr;
            String parPkg = parId.getPackageName();
            for(String cname : cpinfo.getPackageClassNames(parPkg, prefix))
                theSuggestions.add(new JavaDecl(cname, null, null, null));
            for(String pname : cpinfo.getPackageChildrenNames(parPkg, prefix))
                theSuggestions.add(new JavaDecl(JavaDecl.Type.Package, pname));
        }
        
        // Handle anything else with a parent class
        else if(parExpr.getJClass()!=null) { Class pclass = parExpr.getJClass();
            if(pclass.isArray()) // If array, add array field
                theSuggestions.add(new JavaDecl(getLengthField()));
            for(Field field : pclass.getFields()) // Add class fields
                if(StringUtils.startsWithIC(field.getName(), prefix))
                    theSuggestions.add(new JavaDecl(field));
            for(Method method : pclass.getMethods()) // Add class methods
                if(StringUtils.startsWithIC(method.getName(), prefix))
                    theSuggestions.add(new JavaDecl(method));
        }
    }
    
    // If no JExpr prefix, get variables with prefix
    else {
        
        // Get variables with prefix of name and add to suggestions
        List <JVarDecl> varDecls = anId.getVarDecls(prefix, new ArrayList());
        for(JVarDecl vdecl : varDecls)
            theSuggestions.add(vdecl.getDecl());
        
        // Add methods of enclosing class
        JClassDecl ecd = anId.getEnclosingClassDecl();
        Class ec = ecd!=null? ecd.getJClass() : null;
        while(ecd!=null && ec!=null) {
            for(Method meth : ClassUtils.getMethods(ec, prefix))
                theSuggestions.add(new JavaDecl(meth));
            ecd = ecd.getEnclosingClassDecl(); ec = ecd!=null? ecd.getJClass() : null;
        }

        // Add classes with prefix that are public
        List <String> cnames = cpinfo.getAllClassNames(prefix);
        for(String cname : cnames) {
            Class cls = ClassUtils.getClass(cname, proj.getClassLoader());
            if(cls==null || !Modifier.isPublic(cls.getModifiers())) continue;
            theSuggestions.add(new JavaDecl(cname, null, null, null));
        }

        // Add packages with prefix
        List <String> pnames = cpinfo.getAllPackageNames(prefix);
        for(String name : pnames)
            theSuggestions.add(new JavaDecl(JavaDecl.Type.Package, name));
    }
}

/**
 * A bogus "Array" class to provide a bogus field "length" to stand in for non-standard array length field.
 */
class Array { public int length; }
static Field getLengthField() { try { return Array.class.getField("length"); } catch(Exception e) { return null; }}

/** Returns the assignable type of given node assuming it's the receiving expression of assignment or a method arg. */
private static Class getReceivingClass(JNode aNode)
{
    // If MethocCall arg, return arg class
    Class cls = getMethodCallArgClass(aNode);
    if(cls!=null)
        return cls;
    
    // If node is Assignment Right-Hand-Side, return assignment Left-Hand-Side class
    JExprMath assExpr = getExpression(aNode, JExprMath.Op.Assignment);
    JExpr lhs = assExpr!=null? assExpr.getOperand(0) : null;
    if(lhs!=null)
        return lhs.getJClass();
    
    // If node is JVarDecl Initializer, return JVarDecl class
    JVarDecl vd = getVarDeclForInitializer(aNode);
    if(vd!=null)
        return vd.getJClass();

    // Return null since no assignment type found for class
    return null;
}

/** Returns ClassPathInfo for JNode. */
private static ClassPathInfo getClassPathInfo(JNode aNode)
{
    WebFile file = aNode.getFile().getSourceFile(); if(file==null) return null;
    WebSite site = file.getSite();
    return ClassPathInfo.get(site);
}

/** Returns the method call parent of given node, if available. */
private static JExprMethodCall getMethodCall(JNode aNode)
{
    JNode node = aNode;
    while(node!=null && !(node instanceof JStmt) && !(node instanceof JMemberDecl)) {
        if(node instanceof JExprMethodCall)
            return (JExprMethodCall)node;
        node = node.getParent();
    }
    return null;
}

/** Return the method call arg class of node, if node is MethodCall arg. */
private static Class getMethodCallArgClass(JNode aNode)
{
    JExprMethodCall methodCall = getMethodCall(aNode); if(methodCall==null) return null;
    int argIndex = getMethodCallArgIndex(methodCall, aNode); if(argIndex<0) return null;
    Method method = methodCall.getMethod(); if(method==null) return null;
    Class argClasses[] = method.getParameterTypes();
    return argIndex<argClasses.length? argClasses[argIndex] : null;
}

/** Return the method call arg index of node. */
private static int getMethodCallArgIndex(JExprMethodCall aMethodCall, JNode aNode)
{
    JExprMethodCall methodCall = aMethodCall!=null? aMethodCall : getMethodCall(aNode); if(methodCall==null) return -1;
    List <JExpr> args = methodCall.getArgs();
    JNode node = aNode;
    while(node!=methodCall) {
        for(int i=0, iMax=args.size(); i<iMax; i++)
            if(args.get(i)==node)
                return i;
        node = node.getParent();
    }
    return -1;
}

/** Returns the expression for given node with given op, if available. */
private static JExprMath getExpression(JNode aNode, JExprMath.Op anOp)
{
    for(JNode n=aNode; n!=null && !(n instanceof JStmt) && !(n instanceof JMemberDecl); n=n.getParent()) {
        if(n instanceof JExprMath) { JExprMath expr = (JExprMath)n;
            if(expr.op==anOp)
                return expr; }}
    return null;
}

/** Returns the JVarDecl for given node, if node is initializer. */
private static JVarDecl getVarDeclForInitializer(JNode aNode)
{
    JNode node = aNode;
    while(node!=null && !(node instanceof JStmt) && !(node instanceof JMemberDecl)) {
        if(node instanceof JExpr) { JExpr expr = (JExpr)node;
            if(expr.getParent() instanceof JVarDecl) { JVarDecl vd = (JVarDecl)expr.getParent();
                if(vd.getInitializer()==expr)
                    return vd;
            }
        }
        node = node.getParent();
    }
    return null;
}

/**
 * A Comparator to sort JavaDecls.
 */
private static class DeclCompare implements Comparator<JavaDecl> {

    // The project and the class type of node
    Project _proj; Class _rclass = null;
    
    /** Creates a DeclCompare. */
    DeclCompare(Project aProj, Class aRC)  { _proj = aProj; _rclass = aRC; }

    /** Standard compare to method.  */
    public int compare(JavaDecl o1, JavaDecl o2)
    {
        // Get whether either suggestion is of Assignable to ReceivingClass
        boolean rca1 = isRecivingClassAssignable(o1);
        boolean rca2 = isRecivingClassAssignable(o2);
        if(rca1!=rca2) return rca1? -1 : 1;
                
        // If Suggestion Types differ, return by type
        if(o1.getType()!=o2.getType()) return o1.getType().ordinal()<o2.getType().ordinal()? -1 : 1;
        
        // If either is member class, sort other first
        if(o1.isMemberClass()!=o2.isMemberClass()) return o2.isMemberClass()? -1 : 1; 
        
        // Special ClassName support
        if(o1.isClass()) {
            String s1 = o1.getClassName(), s2 = o2.getClassName();
            if(s1.startsWith("java.lang.") && !s2.startsWith("java.lang.")) return -1;
            if(s2.startsWith("java.lang.") && !s1.startsWith("java.lang.")) return 1;
            if(s1.startsWith("java.util.") && !s2.startsWith("java.util.")) return -1;
            if(s2.startsWith("java.util.") && !s1.startsWith("java.util.")) return 1;
            if(s1.startsWith("java.") && !s2.startsWith("java.")) return -1;
            if(s2.startsWith("java.") && !s1.startsWith("java.")) return 1;
        }
        int c = o1.getSimpleName().compareToIgnoreCase(o2.getSimpleName()); if(c!=0) return c;
        return o1.getFullName().compareToIgnoreCase(o2.getFullName());
    }

    /** Returns whether suggestion is receiving class. */
    private boolean isRecivingClassAssignable(JavaDecl aJD)
    {
        if(_rclass==null || aJD.isPackage()) return false;
        String tname = aJD.getTypeName(); if(tname==null) return false;
        Class tcls = ClassUtils.getClass(tname, _proj.getClassLoader()); if(tcls==null) return false;
        return _rclass.isAssignableFrom(tcls);
    }
}

}