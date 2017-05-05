/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snap.javaparse;

/**
 * A Java member for ConstrDecl.
 */
public class JConstrDecl extends JMethodDecl {

/**
 * Override to get declaration from actual Constructor.
 */
protected JavaDecl getDeclImpl()
{
    // Get param types
    JavaDecl ptypes[] = getParamTypes();
    
    // Get parent JClassDecl and JavaDecl
    JClassDecl cd = getEnclosingClassDecl(); if(cd==null) return null;
    JavaDecl cdecl = cd.getDecl();
    
    // Return Constructor for param types
    JavaDeclHpr clsHpr = cdecl.getHpr();
    return clsHpr.getConstructorDecl(ptypes);
}

/**
 * Override to check field declarations for id.
 */
protected JavaDecl getDeclImpl(JNode aNode)
{
    if(aNode==_id) return getDecl();
    return super.getDeclImpl(aNode);
}

/**
 * Returns the member that this member overrides or implements, if available.
 */
protected JavaDecl getSuperDeclImpl()
{
    // Get enclosing class and super class and method parameter types
    JClassDecl ecd = getEnclosingClassDecl(); if(ecd==null) return null;
    Class sclass = ecd.getSuperClass(); if(sclass==null) return null;
    Class ptypes[] = getParamClasses();
    
    // Iterate over superclasses and return if any have method
    for(Class cls=sclass; cls!=null; cls=cls.getSuperclass()) {
        try { return getJavaDecl(cls.getDeclaredConstructor(ptypes)); }
        catch(Exception e) { }
    }
    
    // Return null since not found
    return null;
}

/** Returns the part name. */
public String getNodeString()  { return "ConstrDecl"; }

}