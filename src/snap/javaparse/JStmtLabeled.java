/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snap.javaparse;
import snap.util.SnapUtils;

/**
 * A Java statement for LabledStatement.
 */
public class JStmtLabeled extends JStmt
{
    // The label identifier
    JExprId     _label;
    
    // The actual statement
    JStmt       _stmt;
    
    // The bogus label variable declaration
    JVarDecl    _lvd;

/**
 * Returns the label.
 */
public JExprId getLabel()  { return _label; }

/**
 * Sets the label.
 */
public void setLabel(JExprId anExpr)  { replaceChild(_label, _label=anExpr); }

/**
 * Returns the label name.
 */
public String getLabelName()  { return _label!=null? _label.getName() : null; }

/**
 * Returns a bogus label variable declaration.
 */
public JVarDecl getLabelVarDecl()
{
    if(_lvd!=null) return _lvd;
    JType typ = new JType(); typ._name = "String"; typ._decl = new JavaDecl(String.class);
    _lvd = new JVarDecl(); _lvd._id = _label; _lvd._type = typ;
    return _lvd;
}

/**
 * Returns the statement.
 */
public JStmt getStmt()  { return _stmt; }

/**
 * Sets the statement.
 */
public void setStmt(JStmt aStmt)  { replaceChild(_stmt, _stmt=aStmt); }

/**
 * Override to handle label variable declaration.
 */
@Override
protected JavaDecl resolveName(JNode aNode)
{
    String name = aNode.getName(); boolean isType = aNode instanceof JExprType;
    if(!isType && SnapUtils.equals(getLabelName(), name))
        return getLabelVarDecl().getDecl();
    return super.resolveName(aNode);
}

}