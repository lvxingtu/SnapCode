/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snap.javaparse;
import java.lang.reflect.Member;
import snap.util.ClassUtils;

/**
 * A Java part for import declaration.
 */
public class JImportDecl extends JNode {

    // The import name expression
    JExpr          _nameExpr;
    
    // Whether import is static
    boolean        isStatic;
    
    // Whether import is inclusive (ends with '.*')
    boolean        isInclusive;
    
/**
 * Returns whether import is static.
 */
public boolean isStatic()  { return isStatic; }

/**
 * Sets whether import is static.
 */
public void setStatic(boolean aValue)  { isStatic = aValue; }

/**
 * Returns whether import is inclusive.
 */
public boolean isInclusive()  { return isInclusive; }

/**
 * Sets whether import is inclusive.
 */
public void setInclusive(boolean aValue)  { isInclusive = aValue; }

/**
 * Returns whether import is class name.
 */
public boolean isClassName()  { return getDecl().isClass(); }

/**
 * Returns class or package declaration.
 */
protected JavaDecl getDeclImpl()
{
    String name = getName(); if(name==null) return null;
    if(isInclusive && isKnownPackageName(name)) return new JavaDecl(JavaDecl.Type.Package, name);
    while(name!=null) {
        if(isKnownClassName(name)) {
            if(name.length()<getName().length())
                name = name + getName().substring(name.length()).replace('.', '$');
            return new JavaDecl(name,null,null,null);
        }
        int i = name.lastIndexOf('.'); if(i>0) name = name.substring(0,i); else name = null;
    }
    return new JavaDecl(JavaDecl.Type.Package, getName());
}

/**
 * Returns the class name for a given name.
 */
public String getImportClassName(String aName)
{
    String cname = isClassName()? getClassName() : getName();
    if(isInclusive) {
        if(!isStatic() || !cname.endsWith(aName))
            cname += (isClassName()? '$' : '.') + aName;
    }
    return cname;
}

/**
 * Returns the class for a given name.
 */
public Class getImportClass(String aName)
{
    String cname = getImportClassName(aName);
    return cname!=null? getClassForName(cname) : null;
}

/**
 * Returns the member for given name and parameter types (if method) for static import.
 */
public Member getImportMember(String aName, Class theParams[])
{
    Class cls = getJClass(); if(cls==null) return null;
    if(theParams==null)
        return ClassUtils.getField(cls, aName);
    return ClassUtils.getMethod(cls, aName, theParams);
}

/**
 * Returns the name expression.
 */
public JExpr getNameExpr()  { return _nameExpr; }

/**
 * Sets the name expression.
 */
public void setNameExpr(JExpr anExpr)  { replaceChild(_nameExpr, _nameExpr = anExpr); }

/**
 * Resolves the name from identifier, if available.
 */
protected String getNameImpl()  { return _nameExpr!=null? _nameExpr.getName() : null; }

}