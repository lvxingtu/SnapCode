/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snap.javaparse;

/**
 * A Java part for package declaration.
 */
public class JPackageDecl extends JNode {

    // The package name identifier
    JExpr        _nameExpr;

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
    if(_nameExpr!=null) setName(_nameExpr.getString());
}

}