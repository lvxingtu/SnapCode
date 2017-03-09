/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snap.javaparse;
import java.lang.reflect.*;
import java.util.*;
import snap.util.*;

/**
 * A Java member for MethodDeclaration.
 */
public class JMethodDecl extends JMemberDecl
{
    // The type/return-type
    JType                  _type;

    // Type parameters
    List <JTypeParam>      _typeParams;
    
    // The formal parameters
    List <JVarDecl>        _params = new ArrayList();
    
    // The throws names list
    List <JExpr>           _throwsNameList = new ArrayList();
    
    // The statement Block
    JStmtBlock             _block;

/**
 * Returns the field type.
 */
public JType getType()  { return _type; }

/**
 * Sets the field type.
 */
public void setType(JType aType)  { replaceChild(_type, _type = aType); }

/**
 * Returns the method JTypeParams.
 */
public List <JTypeParam> getTypeParams()  { return _typeParams; }

/**
 * Sets the method JTypeParams.
 */
public void setTypeParams(List <JTypeParam> theTPs)
{
    if(_typeParams!=null) for(JTypeParam tp : _typeParams) removeChild(tp);
    _typeParams = theTPs;
    if(_typeParams!=null) for(JTypeParam tp : _typeParams) addChild(tp, -1);
}

/**
 * Returns the number of formal parameters.
 */
public int getParamCount()  { return _params.size(); }

/**
 * Returns the individual formal parameter at given index.
 */
public JVarDecl getParam(int anIndex)  { return _params.get(anIndex); }

/**
 * Returns the list of formal parameters.
 */
public List <JVarDecl> getParameters()  { return _params; }

/**
 * Returns the list of formal parameters.
 */
public void addParameter(JVarDecl aVD)
{
    if(aVD==null) { System.err.println("JMethodDecl.addParam: Add null param!"); return; }
    _params.add(aVD); addChild(aVD, -1);
}

/**
 * Returns the list of formal parameters.
 */
public Class[] getParametersTypes()
{
    Class ptypes[] = new Class[_params.size()];
    for(int i=0, iMax=_params.size(); i<iMax; i++) { JVarDecl vd = _params.get(i);
        ptypes[i] = vd.getJClass(); }
    return ptypes;
}

/**
 * Returns the throws list.
 */
public List <JExpr> getThrowsList()  { return _throwsNameList; }

/**
 * Sets the throws list.
 */
public void setThrowsList(List <JExpr> theThrows)
{
    if(_throwsNameList!=null) for(JExpr t : _throwsNameList) removeChild(t);
    _throwsNameList = theThrows;
    if(_throwsNameList!=null) for(JExpr t : _throwsNameList) addChild(t, -1);    
}

/**
 * Returns whether statement has a block associated with it.
 */
public boolean isBlock()  { return true; }

/**
 * Returns the block.
 */
public JStmtBlock getBlock()  { return _block; }

/**
 * Sets the block.
 */
public void setBlock(JStmtBlock aBlock)  { replaceChild(_block, _block = aBlock); }

/**
 * Override to get decl from method.
 */
@Override
protected JavaDecl getDeclImpl()
{
    Method meth = getMethod();
    return meth!=null? getJavaDecl(meth) : null;
}

/**
 * Override to check formal parameters.
 */
@Override
protected JavaDecl resolveName(JNode aNode)
{
    // If node is method name, return method decl
    if(aNode==_id)
        return getDecl();
    
    // Iterate over formalParams
    String name = aNode.getName();
    if(aNode instanceof JExprId) for(JVarDecl vd : _params)
        if(SnapUtils.equals(vd.getName(), name))
            return vd.getDecl();
    
    // Look for JTypeParam for given name
    JTypeParam tp = getTypeParam(name);
    if(tp!=null)
        return tp.getDecl();
    
    // Do normal version
    return super.resolveName(aNode);
}

/**
 * Returns a variable with given name.
 */
public List <JVarDecl> getVarDecls(String aPrefix, List<JVarDecl> theVariables)
{
    // Iterate over statements and see if any JStmtVarDecl contains variable with that name
    if(_block!=null)
    for(JStmt s : _block.getStatements()) {
        if(s instanceof JStmtVarDecl) {
            JStmtVarDecl lvds = (JStmtVarDecl)s;
            for(JVarDecl v : lvds.getVarDecls())
                if(StringUtils.startsWithIC(v.getName(), aPrefix))
                    theVariables.add(v);
        }
    }
    
    // Iterate over formalParams
    for(JVarDecl v : _params)
        if(StringUtils.startsWithIC(v.getName(), aPrefix))
            theVariables.add(v);
    
    // Do normal version
    return super.getVarDecls(aPrefix, theVariables);
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
        try { return getJavaDecl(cls.getDeclaredMethod(getName(), ptypes)); }
        catch(Exception e) { }
    }
    
    // Iterate over enclosing class interfaces and return if any have method
    for(Class intf : ecd.getInterfaces())
        try { return getJavaDecl(intf.getDeclaredMethod(getName(), ptypes)); }
        catch(Exception e) { }
    
    // Return null since not found
    return null;
}

/**
 * Returns the java.lang.reflect Method for this method decl from the compiled class.
 */
public Method getMethod()
{
    JClassDecl cd = getEnclosingClassDecl();
    Class cls = cd!=null? cd.getJClass() : null; if(cls==null) return null;
    String name = getName(); if(name==null) return null;
    try { return cls.getDeclaredMethod(getName(), getParametersTypes()); }
    catch(Throwable e) { return null; }
}

/** Returns the part name. */
public String getNodeString()  { return "MethodDecl"; }

}