package snap.javaparse;

/**
 * An class to represent expressions that include an operator (math, logical, etc.).
 */
public class JExprMath extends JExpr {

    // The operator
    public Op         op;

    // Constants for op
    public enum Op {
        Add, Subtract, Multiply, Divide, Mod,
        Equal, NotEqual, LessThan, GreaterThan, LessThanOrEqual, GreaterThanOrEqual,
        Or, And, Not, BitOr, BitXOr, BitAnd, Conditional, Assignment,
        ShiftLeft, ShiftRight, ShiftRightUnsigned,
        PreIncrement, PreDecrement, Negate, BitComp, PostIncrement, PostDecrement
    }
    
/**
 * Creates a new expression.
 */
public JExprMath()  { }

/**
 * Creates a new expression for given op and LeftHand expression.
 */
public JExprMath(Op anOp, JExpr aFirst)  { op = anOp; addOperand(aFirst); }

/**
 * Creates a new expression for given op and LeftHand/RightHand expressions.
 */
public JExprMath(Op anOp, JExpr aFirst, JExpr aSecond)  { op = anOp; addOperand(aFirst); addOperand(aSecond); }

/**
 * Returns the op.
 */
public Op getOp()  { return op; }

/**
 * Returns the operand count.
 */
public int getOperandCount()  { return getChildCount(); }

/**
 * Returns the specified operand.
 */
public JExpr getOperand(int anIndex)  { return (JExpr)getChild(anIndex); }

/**
 * Adds an operand.
 */
public void addOperand(JExpr anExpr)  { addChild(anExpr, -1); }

/**
 * Sets the specified operand.
 */
public void setOperand(JExpr anExpr, int anIndex)
{
    if(anIndex<getChildCount()) replaceChild(getChild(anIndex), anExpr);
    else addChild(anExpr, -1);
}

/**
 * Returns the class name for expression.
 */
protected JavaDecl getDeclImpl()
{
    switch(op) {
        case Add: case Subtract: case Multiply: case Divide: case Mod: return getDeclMath();
        case Equal: case NotEqual: case LessThan: case GreaterThan: case LessThanOrEqual: case GreaterThanOrEqual:
        case Or: case And: case Not: return JavaDecl.BOOL_DECL;
        case Conditional:     // Should probably take common ancestor of both
            return getChildCount()>2? getOperand(1).getDecl() : JavaDecl.OBJECT_DECL;
        case Assignment: return getOperand(0).getDecl();
        case BitOr: case BitXOr: case BitAnd: return getOperand(0).getDecl();
        case ShiftLeft: case ShiftRight: case ShiftRightUnsigned: return getOperand(0).getDecl();
        case PreIncrement: case PreDecrement: case Negate: case BitComp: return getOperand(0).getDecl();
        case PostIncrement: case PostDecrement: return getOperand(0).getDecl();
        default: return JavaDecl.BOOL_DECL;
    }
}

/** Returns the class name for math expression. */
private JavaDecl getDeclMath() { String cname = getClassNameMath(); return cname!=null? getJavaDecl(cname) : null; }
private String getClassNameMath()
{
    String c1 = getChildCount()>0? getOperand(0).getClassName() : null;
    String c2 = getChildCount()>1? getOperand(1).getClassName() : null;
    if(c1==null) return c2; if(c2==null) return c1; if(c1.equals(c2)) return c1;
    if(c1.endsWith("ouble")) return c1; if(c2.endsWith("ouble")) return c2;
    if(c1.endsWith("loat")) return c1; if(c2.endsWith("loat")) return c2;
    if(c1.endsWith("ong")) return c1; if(c2.endsWith("ong")) return c2;
    if(c1.endsWith("nt") || c1.endsWith("Integer")) return c1;
    if(c2.endsWith("nt") || c2.endsWith("Integer")) return c2;
    return c1;
}

/**
 * Returns the part name.
 */
public String getNodeString()  { return op + "Expr"; }

/**
 * Returns the Op string for op.
 */
public static String getOpString(Op anOp)
{
    switch(anOp) {
        case Add: return "+"; case Subtract: return "-";
        case Multiply: return "*"; case Divide: return "/"; case Mod: return "%";
        case Equal: return "=="; case NotEqual: return "!=";
        case LessThan: return "<"; case GreaterThan: return ">";
        case LessThanOrEqual: return "<="; case GreaterThanOrEqual: return ">=";
        case Or: return "||"; case And: return "&&"; case Not: return "!";
        case BitOr: return "|"; case BitXOr: return "^"; case BitAnd: return "&";
        case Conditional: return "?"; case Assignment: return "=";
        case ShiftLeft: return "<<"; case ShiftRight: return ">>"; case ShiftRightUnsigned: return ">>>";
        case PreIncrement: return "++"; case PreDecrement: return "--";
        case Negate: return "-"; case BitComp: return "<DUNNO>";
        case PostIncrement: return "++"; case PostDecrement: return "--";
        default: throw new RuntimeException("JExprMath: Unknown Op: " + anOp);
    }
}

}