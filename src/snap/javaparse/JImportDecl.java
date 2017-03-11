/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snap.javaparse;
import java.lang.reflect.Member;
import java.util.*;
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
    
    // Whether import is used
    boolean        _used;
    
    // The list of child class names found by this import, if inclusive
    Set <String>  _found = Collections.EMPTY_SET;
    
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
    if(isInclusive && isKnownPackageName(name)) return getJavaDecl(name);
    
    // Iterate up parts of import till we find Class in case import is like: import path.Class.InnerClass;
    while(name!=null) {
        if(isKnownClassName(name)) {
            if(name.length()<getName().length()) {
                String icname = getName().substring(name.length()).replace('.','$'), name2 = name + icname;
                if(isKnownClassName(name2))
                    name = name2;
            }
            return getJavaDecl(name);
        }
        int i = name.lastIndexOf('.'); if(i>0) name = name.substring(0,i); else name = null;
    }
    
    // If class not found, return as package decl anyway
    return getJavaDecl(getName());
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
public void setNameExpr(JExpr anExpr)
{
    replaceChild(_nameExpr, _nameExpr = anExpr);
    if(_nameExpr!=null) setName(_nameExpr.getName());
}

/**
 * Returns the list of child class names found by this import (if inclusive).
 */
public Set <String> getFoundClassNames()  { return _found; }

/**
 * Adds a child class name to list of those
 */
protected void addFoundClassName(String aName)
{
    if(_found==Collections.EMPTY_SET) _found = new HashSet();
    _found.add(aName);
}

}