/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snap.javaparse;
import java.lang.reflect.Constructor;
import java.util.*;
import snap.util.ClassUtils;

/**
 * A JExpr subclass for Allocation expressions.
 */
public class JExprAlloc extends JExpr {

    // The Allocation type
    JType         _type;
    
    // The allocation args
    List <JExpr>  _args = Collections.EMPTY_LIST;
    
    // The dimensions expression, if array
    JExpr         _arrayDims;
    
    // The array init expressionss, if array
    List <JExpr>  _arrayInits = Collections.EMPTY_LIST;
    
    // The allocation JClassDecl
    JClassDecl    _classDecl;

/**
 * Returns the allocation JType.
 */
public JType getType()  { return _type; }

/**
 * Sets the allocation JType.
 */
public void setType(JType aType)  { replaceChild(_type, _type = aType); }

/**
 * Returns the allocation arguments.
 */
public List <JExpr> getArgs()  { return _args; }

/**
 * Sets the allocation arguments.
 */
public void setArgs(List <JExpr> theArgs)
{
    if(_args!=null) for(JExpr arg : _args) removeChild(arg);
    _args = theArgs;
    if(_args!=null) for(JExpr arg : _args) addChild(arg, -1);
}

/**
 * Returns the arg classes.
 */
public Class[] getArgClasses()
{
    List <JExpr> args = getArgs();
    Class classes[] = new Class[args.size()];
    for(int i=0, iMax=args.size(); i<iMax; i++) { JExpr arg = args.get(i);
        classes[i] = arg!=null? arg.getJClass() : null; }
    return classes;
}

/**
 * Returns the array dimensions.
 */
public JExpr getArrayDims()  { return _arrayDims; }

/**
 * Sets the array dimension.
 */
public void setArrayDims(JExpr theDims)
{
    replaceChild(_arrayDims, _arrayDims = theDims);
}

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
 * Returns the allocation ClassBodyDecl.
 */
public JClassDecl getClassDecl()  { return _classDecl; }

/**
 * Sets the allocation ClassBodyDecl.
 */
public void setClassDecl(JClassDecl aCD)  { replaceChild(_classDecl, _classDecl = aCD); }

/**
 * Returns the part name.
 */
public String getNodeString()  { return "Allocation"; }

/**
 * Tries to resolve the declaration for this node.
 */
protected JavaDecl getDeclImpl()
{
    Constructor cstr = getConstructor();
    return cstr!=null? new JavaDecl(cstr) : null;
}

/**
 * Returns the contructor by querying parent class ref.
 */
public Constructor getConstructor()
{
    Class cls = _type!=null? _type.getJClass() : null;
    try { return cls!=null? ClassUtils.getConstructor(cls, getArgClasses()) : null; }
    catch(Throwable t) { return null; }
}

}