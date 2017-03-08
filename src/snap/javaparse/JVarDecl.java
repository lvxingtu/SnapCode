/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snap.javaparse;
import java.lang.reflect.Field;
import java.util.*;

/**
 * A JNode to represent a defined variable.
 * Found in JFieldDecl, JStmtVarDecl, Method/Catch FormalParam(s)
 */
public class JVarDecl extends JNode {

    // The type
    JType          _type;

    // The variable name
    JExprId        _id;
    
    // The variable dimension (if defined with variable instead of type)
    int            _arrayCount;
    
    // The initializer
    JExpr          _initializer;
    
    // The array initializer (if array)
    List <JExpr>   _arrayInits = Collections.EMPTY_LIST;

/**
 * Resolves the name from identifier.
 */
protected String getNameImpl()  { return _id!=null? _id.getName() : null; }

/**
 * Returns whether type is set.
 */
public boolean isTypeSet()  { return _type!=null; }

/**
 * Returns the type.
 */
public JType getType()
{
    if(_type!=null) return _type;
    JNode par = getParent();
    if(par instanceof JFieldDecl) return ((JFieldDecl)par).getType();
    if(par instanceof JStmtVarDecl) return ((JStmtVarDecl)par).getType();
    return null;
}

/**
 * Sets the type.
 */
public void setType(JType aType)  { replaceChild(_type, _type=aType); }

/**
 * Returns the identifier.
 */
public JExprId getId()  { return _id; }

/**
 * Sets the identifier.
 */
public void setId(JExprId anId)  { replaceChild(_id, _id=anId); }

/**
 * Returns the array count.
 */
public int getArrayCount()  { return _arrayCount; }

/**
 * Sets the array count.
 */
public void setArrayCount(int aValue)  { _arrayCount = aValue; }

/**
 * Returns the initializer.
 */
public JExpr getInitializer()  { return _initializer; }

/**
 * Sets the initializer.
 */
public void setInitializer(JExpr anExpr)  { replaceChild(_initializer, _initializer=anExpr); }

/**
 * Returns the array init expressions, if array.
 */
public List <JExpr> getArrayInits()  { return _arrayInits; }

/**
 * Sets the array init expressions, if array.
 */
public void setArrayInits(List <JExpr> theArrayInits)
{
    if(_arrayInits!=null) for(JExpr expr : _arrayInits) removeChild(expr);
    _arrayInits = theArrayInits;
    if(_arrayInits!=null) for(JExpr expr : _arrayInits) addChild(expr, -1);
}

/**
 * Returns the declaring class, if field variable.
 */
public Class getDeclaringClass()
{
    return getParent() instanceof JFieldDecl? getParent(JClassDecl.class).getJClass() : null;
}

/**
 * Tries to resolve the class declaration for this node.
 */
protected JavaDecl getDeclImpl()
{
    Field field = getField();
    if(field!=null)
        return new JavaDecl(field);
    return new JavaDecl(this);
}

/**
 * Returns the class name.
 */
public String getClassName()
{
    // Get name from type
    JType typ = getType(); if(typ==null) return null;
    String cname = typ.getClassName();
    if(cname==null)
        System.err.println("JVarDecl.getClassName: Can't find class for " + getName() + " in " + getFile().getName());
    
    // Handle array
    if(_arrayCount>0) {
        String pfix = "["; for(int i=1; i<_arrayCount; i++) pfix += "[";
        if(typ.getArrayCount()>0) cname = pfix + cname;
        else if(typ.isPrimitive()) cname = pfix + String.valueOf(Character.toUpperCase(cname.charAt(0)));
        else cname = pfix + "L" + cname + ";";
    }
    
    // Return name
    return cname;
}

/**
 * Override to resolve id node.
 */
@Override
protected JavaDecl resolveName(JNode aNode)
{
    if(aNode==_id)
        return getDecl();
    return super.resolveName(aNode);
}

/**
 * Returns the java.lang.reflect Field for this field decl from the compiled class.
 */
public Field getField()
{
    if(!(getParent() instanceof JFieldDecl)) return null;
    JClassDecl cd = getEnclosingClassDecl();
    Class cls = cd!=null? cd.getJClass() : null; if(cls==null) return null;
    try { return cls.getDeclaredField(getName()); }
    catch(Throwable e) { return null; }
}

}