/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snap.javaparse;
import java.lang.reflect.Constructor;

/**
 * A Java member for ConstrDecl.
 */
public class JConstrDecl extends JMethodDecl {

/**
 * Override to get declaration from actual Constructor.
 */
@Override
protected JavaDecl getDeclImpl()
{
    Constructor c = getConstructor();
    return c!=null? new JavaDecl(c) : null;
}

/**
 * Returns the java.lang.reflect Constructor for this method decl from the compiled class.
 */
public Constructor getConstructor()
{
    JClassDecl cd = getEnclosingClassDecl();
    Class cls = cd!=null? cd.getJClass() : null; if(cls==null) return null;
    try { return cls.getDeclaredConstructor(getParametersTypes()); }
    catch(NoSuchMethodException e) { return null; }
}

/**
 * Returns the member that this member overrides or implements, if available.
 */
protected JavaDecl getSuperDeclImpl()
{
    // Get enclosing class and super class and method parameter types
    JClassDecl ecd = getEnclosingClassDecl(); if(ecd==null) return null;
    Class sclass = ecd.getSuperClass(); if(sclass==null) return null;
    Class ptypes[] = getParametersTypes();
    
    // Iterate over superclasses and return if any have method
    for(Class cls=sclass; cls!=null; cls=cls.getSuperclass()) {
        try { return new JavaDecl(cls.getDeclaredConstructor(ptypes)); }
        catch(Exception e) { }
    }
    
    // Return null since not found
    return null;
}

/** Returns the part name. */
public String getNodeString()  { return "ConstrDecl"; }

}