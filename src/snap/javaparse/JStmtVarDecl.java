/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snap.javaparse;
import java.util.*;

/**
 * A Java statement for local variable declarations.
 */
public class JStmtVarDecl extends JStmt
{
    // The modifiers
    JModifiers        _mods;
    
    // The type/return-type
    JType             _type;
    
    // List of variable declarations
    List <JVarDecl>   _varDecls = new ArrayList();

/**
 * Returns the modifiers.
 */
public JModifiers getMods()  { return _mods; }

/**
 * Sets the modifiers.
 */
public void setMods(JModifiers theMods)
{
    if(_mods==null) addChild(_mods=theMods);
    else replaceChild(_mods, _mods=theMods);
}

/**
 * Returns the type.
 */
public JType getType()  { return _type; }

/**
 * Sets the type.
 */
public void setType(JType aType)  { replaceChild(_type, _type=aType); }

/**
 * Returns the variable declarations.
 */
public List <JVarDecl>  getVarDecls()  { return _varDecls; }

/**
 * Adds a variable declaration.
 */
public void addVarDecl(JVarDecl aVD)  { _varDecls.add(aVD); addChild(aVD, -1); }

}