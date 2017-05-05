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

/** Returns the part name. */
public String getNodeString()  { return "ConstrDecl"; }

}