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
 * Returns whether type is set.
 */
public boolean isTypeSet()  { return _type!=null; }

/**
 * Returns the type.
 */
public JType getType()
{
    if(_type!=null) return _type;
    
    // Get parent type from JFieldDecl, JStmtVarDecl
    _type = getParentType(); if(_type==null) return null;
    
    // If array count is set, replace with type to account for it
    if(_arrayCount>0) {
        JType type2 = new JType(); type2._name = _type._name; type2._startToken = type2._endToken = _startToken;
        type2._primitive = _type._primitive; type2._arrayCount = _type._arrayCount + _arrayCount;
        _type = type2; _type._parent = this;
    }
    
    // Return type
    return _type;
}

/**
 * Sets the type.
 */
public void setType(JType aType)  { replaceChild(_type, _type=aType); }

/**
 * Returns the parent type (JFieldDecl, JStmtVarDecl).
 */
private JType getParentType()
{
    JNode par = getParent();
    if(par instanceof JFieldDecl) return ((JFieldDecl)par).getType();
    if(par instanceof JStmtVarDecl) return ((JStmtVarDecl)par).getType();
    return null;
}

/**
 * Returns the identifier.
 */
public JExprId getId()  { return _id; }

/**
 * Sets the identifier.
 */
public void setId(JExprId anId)
{
    replaceChild(_id, _id=anId);
    if(_id!=null) setName(_id.getName());
}

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
    return getParent() instanceof JFieldDecl? getParent(JClassDecl.class).getEvalClass() : null;
}

/**
 * Tries to resolve the class declaration for this node.
 */
protected JavaDecl getDeclImpl()
{
    // If part of a JFieldDecl, get JavaDecl for field
    JNode par = getParent();
    if(par instanceof JFieldDecl) {
        JClassDecl cd = getEnclosingClassDecl(); if(cd==null) return null;
        JavaDecl decl = cd.getDecl(); if(decl==null) {System.err.println("JVarDecl.getDeclImp: No class");return null;}
        JavaDeclHpr declHpr = decl.getHpr();
        JavaDecl fdecl = declHpr.getFieldDeep(getName());
        return fdecl;
    }
    
    // Otherwise, return JavaDecl for this JVarDecl
    return getJavaDecl(this);
}

/**
 * Override to resolve id node.
 */
protected JavaDecl getDeclImpl(JNode aNode)
{
    if(aNode==_id) return getDecl();
    return super.getDeclImpl(aNode);
}

/**
 * Returns the java.lang.reflect Field for this field decl from the compiled class.
 */
public Field getField()
{
    if(!(getParent() instanceof JFieldDecl)) return null;
    JClassDecl cd = getEnclosingClassDecl();
    Class cls = cd!=null? cd.getEvalClass() : null; if(cls==null) return null;
    try { return cls.getDeclaredField(getName()); }
    catch(Throwable e) { return null; }
}

}