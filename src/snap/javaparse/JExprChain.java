/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snap.javaparse;
import java.lang.reflect.Field;
import java.util.*;
import snap.util.ClassUtils;

/**
 * A class to represent a chain of expressions.
 */
public class JExprChain extends JExpr {

/**
 * Creates a new JExprChain.
 */
public JExprChain()  { }

/**
 * Creates a new JExprChain for given parts.
 */
public JExprChain(JExpr ... theExprs)  { for(JExpr expr : theExprs) addExpr(expr); }

/**
 * Returns the name for Identifier or Method.
 */
protected String getNameImpl()
{
    StringBuffer sb = new StringBuffer();
    if(getExprCount()>0) sb.append(getExpr(0).getName());
    for(int i=1, iMax=getExprCount(); i<iMax; i++) sb.append('.').append(getExpr(i).getName());
    return sb.toString();
}

/**
 * Returns the number of expressions.
 */
public int getExprCount()  { return _children.size(); }

/**
 * Returns the individual expression at given index.
 */
public JExpr getExpr(int anIndex)  { return (JExpr)_children.get(anIndex); }

/**
 * Returns the expressions list.
 */
public List <JExpr> getExpressions()  { return (List)_children; }

/**
 * Adds a expression to this JExprChain.
 */
public void addExpr(JExpr anExpr)  { addChild(anExpr, getChildCount()); }

/**
 * Tries to resolve the class declaration for this node.
 */
protected JavaDecl getDeclImpl()
{   
    int pc = getExprCount();
    JExpr p = pc>0? getExpr(pc-1) : null;
    return p!=null? p.getDecl() : null;
}

/**
 * Override to resolve names in chain.
 */
@Override
protected JavaDecl resolveName(JNode aNode)
{
    // Get node info
    String name = aNode.getName(); boolean isId = aNode instanceof JExprId, isType = !isId;
    if(isType)
        return super.resolveName(aNode);
    
    // Get parent expression - if not found (first in chain) do normal version
    JExprId id = (JExprId)aNode;
    JExpr parExpr = id.getParentExpr();
    if(parExpr==null)
        return super.resolveName(aNode);
    
    // Get parent declaration
    JavaDecl parDecl = parExpr.getDecl();
    if(parDecl==null) {
        System.err.println("JExprChain.resolve: No parent decl for " + getName() + " in " + getName()); return null; }
    
    // Handle Parent is Package: Look for package sub-package or package class
    if(parDecl.isPackage()) {
        String pname = parDecl.getPackageName(), pname2 = pname + '.' + name;
        if(isKnownPackageName(pname2))
            return new JavaDecl(JavaDecl.Type.Package, pname2);
        String cname = pname + '.' + name;
        if(isKnownClassName(cname))
            return new JavaDecl(cname, null, null, null);
    }
        
    // Handle Parent is Class: Look for ".this", ".class", static field or inner class
    else if(parDecl.isClass()) {
        Class pclass = parExpr.getJClass();
        if(name.equals("this"))
            return parDecl; // was FieldName
        if(name.equals("class"))
            return new JavaDecl(Class.class); // was FieldName
        Class cls = pclass!=null? ClassUtils.getClass(pclass, name) : null;
        if(cls!=null)
            return new JavaDecl(cls); // was ClassName
        Field field = pclass!=null? ClassUtils.getField(pclass, name) : null;
        if(field!=null) // && Modifier.isStatic(field.getModifiers()))
            return new JavaDecl(field); // was FieldName
    }
    
    // Handle any parent with class: Look for field
    else if(parExpr.getJClass()!=null) { Class pclass = parExpr.getJClass();
        if(pclass.isArray() && name.equals("length"))
            return new JavaDecl(int.class); // was FieldName;
        Field field = pclass!=null? ClassUtils.getField(pclass, name) : null;
        if(field!=null)
            return new JavaDecl(field); // was FieldName;
    }

    // Do normal version
    return super.resolveName(aNode);
}

/**
 * Returns the part name.
 */
public String getNodeString()  { return "ExprChain"; }

}