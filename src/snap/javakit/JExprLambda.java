package snap.javakit;
import java.util.*;
import snap.util.ListUtils;
import snap.util.SnapUtils;

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
 * Returns the number of paramters.
 */
public int getParamCount()  { return _params.size(); }

/**
 * Returns the parameter at given index.
 */
public JVarDecl getParam(int anIndex)  { return _params.get(anIndex); }

/**
 * Adds a formal parameter.
 */
public void addParam(JVarDecl aVD)  { _params.add(aVD); addChild(aVD, -1); }

/**
 * Returns the parameter with given name.
 */
public JVarDecl getParam(String aName)
{
    for(JVarDecl vd : _params) if(SnapUtils.equals(vd.getName(), aName)) return vd;
    return null;
}

/**
 * Returns the list of parameter classes.
 */
public Class[] getParamTypes()
{
    Class ptypes[] = new Class[_params.size()];
    for(int i=0, iMax=_params.size(); i<iMax; i++) { JVarDecl vd = _params.get(i);
        ptypes[i] = vd.getEvalClass(); }
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
 * Override to try to resolve decl from parent.
 */
protected JavaDecl getDeclImpl()
{
    JNode par = getParent();
    if(par==null) // || par._decl==null)
        return null;
    
    // Handle parent is method call: Get lambda interface from method call decl param
    JavaDecl idecl = null;
    if(par instanceof JExprMethodCall) { JExprMethodCall mcall = (JExprMethodCall)par;
        JavaDecl mdecl = par.getDecl(); if(mdecl==null) return null;
        int ind = ListUtils.indexOfId(mcall.getArgs(), this); if(ind<0) return null;
        idecl = mdecl.getParamType(ind);
    }
    
    // Handle parent anything else (JVarDecl, ?): Get lambda interface from eval type
    else if(par!=null)
        idecl = par.getEvalType();
        
    // If type is interface, get lambda type
    if(idecl!=null)
        idecl = idecl.getClassType();
    if(idecl!=null && idecl.isInterface()) {
        JavaDecl mdecl = idecl.getHpr().getLambdaMethod(getParamCount());
        return mdecl;
    }
        
    // Return null since not found
    return null;
}

/**
 * Override to check lambda parameters.
 */
protected JavaDecl getDeclImpl(JNode aNode)
{
    // If node is paramter name, return param decl
    String name = aNode.getName();
    if(aNode instanceof JExprId) { JVarDecl param = getParam(name);
        if(param!=null)
            return param.getDecl(); }
    
    // Do normal version
    return super.getDeclImpl(aNode);
}

/**
 * Returns the node name.
 */
public String getNodeString()  { return "LambdaExpr"; }

}