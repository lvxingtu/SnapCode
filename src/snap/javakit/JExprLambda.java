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
    
    // The declaration for the actual method for the interface this lambda represents
    JavaDecl          _meth;

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
 * Returns the specific method in the lambda class interface that is to be called.
 */
public JavaDecl getMethod()  { getDecl(); return _meth; }

/**
 * Override to try to resolve decl from parent.
 */
protected JavaDecl getDeclImpl()
{
    JNode par = getParent(); if(par==null) return null;
    
    // Handle parent is method call: Get lambda interface from method call decl param
    JavaDecl idecl = null;
    if(par instanceof JExprMethodCall) { JExprMethodCall mcall = (JExprMethodCall)par;
        List <JavaDecl> meths = getCompatibleMethods(); if(meths.size()==0) return null;
        int ind = ListUtils.indexOfId(mcall.getArgs(), this), argc = getParamCount(); if(ind<0) return null;
        for(JavaDecl mdecl : meths) {
            JavaDecl pdecl = mdecl.getParamType(ind); pdecl = pdecl.getClassType();
            _meth = pdecl.getHpr().getLambdaMethod(argc);
            if(_meth!=null)
                return pdecl;
        }
        return null;
    }
    
    // Handle parent anything else (JVarDecl, ?): Get lambda interface from eval type
    else if(par!=null)
        idecl = par.getEvalType();
        
    // If type is interface, get lambda type
    if(idecl!=null)
        idecl = idecl.getClassType();
    if(idecl!=null && idecl.isInterface()) {
        _meth = idecl.getHpr().getLambdaMethod(getParamCount());
        return _meth;
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
    if(aNode instanceof JExprId) {
        String name = aNode.getName();
        JVarDecl param = getParam(name);
        if(param!=null)
            return param.getDecl();
    }
    
    // If node is parameter, get from lambda method params
    if(aNode instanceof JVarDecl && getParent()==this) {
        int ind = ListUtils.indexOfId(_params, aNode);
        if(ind>=0) {
            JavaDecl meth = getMethod(); if(meth==null) return null;
            if(ind<meth.getParamCount())
                return meth.getParamType(ind);
        }
    }
    
    // Do normal version
    return super.getDeclImpl(aNode);
}

/**
 * Returns the method decl for the parent method call (assumes this lambda is an arg).
 */
protected List <JavaDecl> getCompatibleMethods()
{
    // Get method call, method name and args
    JExprMethodCall mc = (JExprMethodCall)getParent();
    String name = mc.getName();
    List <JExpr> args = mc.getArgs();
    int argc = args.size();
    
    // Get arg types
    JavaDecl argTypes[] = new JavaDecl[argc];
    for(int i=0;i<argc;i++) { JExpr arg = args.get(i);
        argTypes[i] = arg instanceof JExprLambda? null : arg.getEvalType(); }
        
    // Get scope node class type and search for compatible method for name and arg types
    JavaDecl sndecl = mc.getScopeNodeEvalType(); if(sndecl==null) return null;
    JavaDecl snct = sndecl.getClassType();
    List <JavaDecl> decls = snct.getHpr().getCompatibleMethodsAll(name, argTypes);
    if(decls.size()>0)
        return decls;
        
    // If code node class type is member class and not static, go up parent classes
    while(snct.isMemberClass() && !snct.isStatic()) {
        snct = snct.getParent();
        decls = snct.getHpr().getCompatibleMethodsAll(name, argTypes);
        if(decls.size()>0)
            return decls;
    }
        
    // See if method is from static import
    //decl = getFile().getImportClassMember(name, argTypes);
    //if(decl!=null && decl.isMethod()) return decl;
        
    // Return null since not found
    return decls;
}

/**
 * Returns the compatible methods.
 */

/**
 * Returns the node name.
 */
public String getNodeString()  { return "LambdaExpr"; }

}