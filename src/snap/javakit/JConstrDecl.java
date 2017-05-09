/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snap.javakit;
import snap.util.ArrayUtils;

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
    JavaDecl ptypes[] = getParamClassTypesSafe();
    
    // Get parent JClassDecl and JavaDecl
    JClassDecl cd = getEnclosingClassDecl(); if(cd==null) return null;
    JavaDecl cdecl = cd.getDecl();
    
    // If inner class and not static, add implied class type to arg types array
    if(cdecl.isMemberClass() && !cdecl.isStatic())
        ptypes = ArrayUtils.add(ptypes, cdecl.getParent(), 0);
    
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