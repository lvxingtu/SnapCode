/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snap.javaparse;

/**
 * A Java part for package declaration.
 */
public class JPackageDecl extends JNode {

    // The modifiers
    JModifiers   _mods;
    
    // The package name identifier
    JExpr        _id;

/**
 * Returns the package name identifier.
 */
public JExpr getIdentifier()  { return _id; }

/**
 * Sets the package name identifier.
 */
public void setIdentifier(JExpr anId)  { replaceChild(_id, _id = anId); }

/**
 * Returns the modifiers.
 */
public JModifiers getModifiers() { return _mods; }

/**
 * Sets the modifiers.
 */
public void setModifiers(JModifiers aValue)
{
    if(_mods==null) addChild(_mods=aValue);
    else replaceChild(_mods, _mods=aValue);
}

/**
 * Resolves the package name from name identifier, if available.
 */
protected String getNameImpl()  { return _id!=null? _id.getName() : null; }

}