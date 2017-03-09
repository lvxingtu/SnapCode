/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snap.javaparse;
import java.lang.reflect.Field;
import java.util.*;
import snap.util.ClassUtils;

/**
 * A Java statement for SwitchStatement.
 */
public class JStmtSwitch extends JStmt
{
    // The expression
    JExpr               _expr;
    
    // The list of SwitchLabels
    List <SwitchLabel>  _switchLabels = new ArrayList();
    
/**
 * Returns the expression.
 */
public JExpr getExpr()  { return _expr; }
    
/**
 * Sets the expression.
 */
public void setExpr(JExpr anExpr)  { replaceChild(_expr, _expr=anExpr); }

/**
 * Returns the switch labels.
 */
public List<SwitchLabel> getSwitchLabels()  { return _switchLabels; }

/**
 * Adds a switch label.
 */
public void addSwitchLabel(SwitchLabel aSL)  { _switchLabels.add(aSL); addChild(aSL, -1); }

/**
 * Override to see if id is enum name.
 */
@Override
protected JavaDecl resolveName(JNode aNode)
{
    // Get node info
    String name = aNode.getName(); boolean isType = aNode instanceof JExprType;
    
    // See if it's a switch label enum
    if(!isType && _expr!=null && aNode.getStart()>getExpr().getEnd()) {
        Class cls = getExpr().getJClass();
        Field field = cls!=null? ClassUtils.getField(cls, name) : null;
        if(field!=null)
            return getJavaDecl(field);
    }

    // Do normal version
    return super.resolveName(aNode);
}

/**
 * A class to represent individual labels in a switch statement.
 */
public static class SwitchLabel extends JNode
{
    // Whether label is default
    boolean           _default;
    
    // The expression
    JExpr             _expr;
    
    // The block statements
    List <JStmt>      _stmts = new ArrayList();
    
    /** Returns whether label is default. */
    public boolean isDefault()  { return _default; }
    
    /** Sets whether label is default. */
    public void setDefault(boolean aValue)  { _default = aValue; }
    
    /** Returns the expression. */
    public JExpr getExpr()  { return _expr; }
        
    /** Sets the expression. */
    public void setExpr(JExpr anExpr)  { replaceChild(_expr, _expr=anExpr); }
    
    /** Returns the statements. */
    public List <JStmt> getStatements()  { return _stmts; }
    
    /** Adds a statement. */
    public void addStatement(JStmt aStmt)  { _stmts.add(aStmt); addChild(aStmt, -1); }
    
    /** Override to check inner variable declaration statements. */
    protected JavaDecl resolveName(JNode aNode)
    {
        JavaDecl decl = JStmtBlock.resolveName(aNode, getStatements());
        if(decl!=null)
            return decl;
        return super.resolveName(aNode);
    }
}

}