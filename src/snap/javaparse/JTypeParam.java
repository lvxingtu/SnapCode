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
public JExprId getIdentifier()  { return _id; }

/**
 * Sets the identifier.
 */
public void setIdentifier(JExprId anId)  { replaceChild(_id, _id = anId); }

/**
 * Returns the types.
 */
public List <JType> getTypes()  { return _types; }

/**
 * Adds a type.
 */
public void addType(JType aType)  { _types.add(aType); addChild(aType, -1); }

/**
 * Resolves the name.
 */
protected String getNameImpl()  { return _id!=null? _id.getName() : null; }

/**
 * Returns a class name of first type (not sure I need to do this).
 */
protected JavaDecl getDeclImpl()  { return _types.size()>0? _types.get(0).getDecl() : JavaDecl.OBJECT_DECL; }

}
