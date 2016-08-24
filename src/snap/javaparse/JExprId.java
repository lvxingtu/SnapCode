package snap.javaparse;

/**
 * A JExpr subclass for identifiers.
 */
public class JExprId extends JExpr {

/**
 * Creates a new identifier.
 */
public JExprId()  { }

/**
 * Creates a new identifier for given value.
 */
public JExprId(String aName)  { setName(aName); }

/**
 * Sets the name for Identifier or Method.
 */
public void setName(String aName)  { _name = aName; }

/**
 * Override to resolve id name from parents.
 */
protected JavaDecl getDeclImpl()
{
    JavaDecl decl = resolveName(this);
    //if(decl==null)
    //    System.err.println("Unresolved: " + getFile().getSourceFile() + " :" + getLineIndex() + " " + getName());
    return decl;
}

/**
 * Returns whether this is variable identifier.
 */
public boolean isVariableId()  { JavaDecl jd = getDecl(); return jd!=null && jd.isVarDecl(); }

/** Returns whether this is Class identifier. */
//public boolean isClassName()  { JavaDecl jd = getDecl(); return jd!=null && jd.isClass(); }
/**  Returns whether this is ClassField identifier. */
//public boolean isFieldName()  { JavaDecl jd = getDecl(); return jd!=null && jd.isField(); }

/**
 * Returns whether this identifier is a method name.
 */
public boolean isMethodName()  { JavaDecl jd = getDecl(); return jd!=null && jd.isMethod(); }

/**
 * Returns the method call if parent is method call.
 */
public JExprMethodCall getMethodCall()
{
    JNode p = getParent(); return p instanceof JExprMethodCall? (JExprMethodCall)p : null;
}

/**
 * Returns the method declaration if parent is method declaration.
 */
public JMethodDecl getMethodDecl()  { JNode p = getParent(); return p instanceof JMethodDecl? (JMethodDecl)p : null; }

/**
 * Returns whether this is package identifier.
 */
public boolean isPackageName()  { JavaDecl jd = getDecl(); return jd!=null && jd.isPackage(); }

/**
 * Returns the full package name for this package identifier.
 */
public String getPackageName()  { return getDecl().getPackageName(); }

/**
 * Returns the part name.
 */
public String getNodeString()
{
    if(getDecl()==null) return "UnknownId";
    switch(getDecl().getType()) {
        case Class: return "ClassId";
        case Field: return "ClassFieldId";
        case Method: return "MethodId";
        case Package: return "PackageId";
        case VarDecl: return "VariableId";
        default: return "UnknownId";
    }
}

}