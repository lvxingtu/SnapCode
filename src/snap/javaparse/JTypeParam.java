/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snap.javaparse;
import java.util.*;

/**
 * A JNode subclass for type parameters.
 */
public class JTypeParam extends JNode {

    // The name identifier
    JExprId        _id;
    
    // The list of types
    List <JType>   _types = new ArrayList();

/**
 * Returns the identifier.
 */
public JExprId getId()  { return _id; }

/**
 * Sets the identifier.
 */
public void setId(JExprId anId)
{
    replaceChild(_id, _id = anId);
    if(_id!=null) setName(_id.getName());
}

/**
 * Returns the types.
 */
public List <JType> getTypes()  { return _types; }

/**
 * Adds a type.
 */
public void addType(JType aType)  { _types.add(aType); addChild(aType, -1); }

/**
 * Returns a class name of first type (not sure I need to do this).
 */
protected JavaDecl getDeclImpl()  { return _types.size()>0? _types.get(0).getDecl() : getJavaDecl(Object.class); }

}
