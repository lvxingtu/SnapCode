package snap.javaparse;
import java.util.*;

/**
 * A JExpr to represent lambda expressions.
 */
public class JExprLambda extends JExpr {

    // The parameters
    List <JVarDecl>   _params = new ArrayList();

    // The expression, if lambda has expression
    JExpr             _expr;
    
    // The statement Block, if lambda has block
    JStmtBlock        _block;

/**
 * Returns the list of formal parameters.
 */
public List <JVarDecl> getParams()  { return _params; }

/**
 * Returns the parameter at given index.
 */
public JVarDecl getParam(int anIndex)  { return _params.get(anIndex); }

/**
 * Adds a formal parameter.
 */
public void addParam(JVarDecl aVD)  { _params.add(aVD); addChild(aVD, -1); }

/**
 * Returns the list of parameter classes.
 */
public Class[] getParamTypes()
{
    Class ptypes[] = new Class[_params.size()];
    for(int i=0, iMax=_params.size(); i<iMax; i++) { JVarDecl vd = _params.get(i);
        ptypes[i] = vd.getJClass(); }
    return ptypes;
}

/**
 * Returns the expression, if lambda has expression.
 */
public JExpr getExpr()  { return _expr; }
    
/**
 * Sets the expression.
 */
public void setExpr(JExpr anExpr)  { replaceChild(_expr, _expr=anExpr); }
    
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
 * Returns the node name.
 */
public String getNodeString()  { return "LambdaExpr"; }

}