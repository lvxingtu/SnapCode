package snap.javakit;
import java.util.*;
import snap.parse.Parser;
import snap.util.SnapUtils;

/**
 * A class to evaluate expressions.
 */
public class EvalExpr {
    
    // The current "this" object
    protected Object    _thisObj;

    // A parser to parse expressions
    static Parser       _exprParser = JavaParser.getShared().getExprParser();
    
/**
 * Evaluate expression.
 */
public Object eval(String anExpr)
{
    _exprParser.setInput(anExpr);
    Object oref = thisObject();
    JExpr expr = _exprParser.parseCustom(JExpr.class);
    Object value; try { value = evalExpr(oref, expr); }
    catch(Exception e) { return e; }
    return value;
}

/**
 * Evaluate JExpr.
 */
public Object evalExpr(Object anOR, JExpr anExpr) throws Exception
{
    if(anExpr instanceof JExprLiteral) return evalLiteral((JExprLiteral)anExpr);
    if(anExpr instanceof JExprId) return evalIdentifier(anOR, (JExprId)anExpr);
    if(anExpr instanceof JExprMethodCall) return evalMethod(anOR, (JExprMethodCall)anExpr);
    if(anExpr instanceof JExprMath) return evalMathExpr(anOR, (JExprMath)anExpr);
    if(anExpr instanceof JExprArrayIndex) return evalArrayIndex(anOR, (JExprArrayIndex)anExpr);
    if(anExpr instanceof JExprChain) return evalExprChain(anOR, (JExprChain)anExpr);
    return null;
}

/**
 * Evaluate JLiteral.
 */
Object evalLiteral(JExprLiteral aLiteral) throws Exception
{
    switch(aLiteral.getLiteralType()) {
        case Boolean: return (Boolean)aLiteral.getValue();
        case Integer: return (Integer)aLiteral.getValue();
        case Long: return (Long)aLiteral.getValue();
        case Float: return (Float)aLiteral.getValue();
        case Double: return (Double)aLiteral.getValue();
        case Character: return (Character)aLiteral.getValue();
        case String: return (String)aLiteral.getValue();
        case Null: return null;
        default: throw new RuntimeException("No Literal Type");
    }
}

/**
 * Evaluate JIdentifier.
 */
Object evalIdentifier(Object anOR, JExprId anId) throws Exception
{
    String name = anId.getName();
    return evalName(anOR, name);
}

/**
 * Evaluate JIdentifier.
 */
Object evalName(Object anOR, String aName) throws Exception
{
    // If name is "this", return ThisObject
    if(aName==null) return null;
    if(aName.equals("this")) return thisObject();
    
    // Check for local variable
    //StackFrame frame = anApp.getCurrentFrame();
    //LocalVariable lvar = frame.visibleVariableByName(name);
    //if(lvar!=null) return frame.getValue(lvar);
    
    // Check for field
    //ReferenceType refType = anOR.referenceType();
    //Field field = refType.fieldByName(name);
    //if(field!=null) return anOR.getValue(field);
    
    // Complain
    throw new RuntimeException("Identifier not found: " + aName);
}

/**
 * Evaluate JMethodCall.
 */
Object evalMethod(Object anOR, JExprMethodCall anExpr) throws Exception
{
    Object thisObj = thisObject();
    List <Object> args = new ArrayList();
    for(JExpr arg : anExpr.getArgs())
        args.add(evalExpr(thisObj, arg));
    return invokeMethod(anOR, anExpr.getName(), args);
}

/**
 * Evaluate JExprArrayIndex.
 */
Object evalArrayIndex(Object anOR, JExprArrayIndex anExpr) throws Exception
{
    // Get Array
    JExpr arrayExpr = anExpr.getArrayExpr();
    Object val = evalExpr(anOR, arrayExpr); if(!isArray(val)) return null;
    Object aref[] = (Object[])val;
    
    // Get Index
    JExpr indexExpr = anExpr.getIndexExpr();
    Object thisObj = thisObject();
    val = evalExpr(thisObj, indexExpr); if(!isPrimitive(val)) return null;
    int index = intValue(val);
    
    // Return ArrayReference value at index
    return arrayValue(aref, index);
}

/**
 * Evaluate JExprChain.
 */
Object evalExprChain(Object anOR, JExprChain anExpr) throws Exception
{
    Object val = anOR; //Object or = anOR; 
    for(int i=0, iMax=anExpr.getExprCount(); i<iMax; i++) { JExpr expr = anExpr.getExpr(i);
        val = evalExpr(val, expr); //val = evalExpr(or, expr);
        //if(val instanceof ObjectReference) or = (ObjectReference)val;
    }
    return val;
}

/**
 * Evaluate JExprMath.
 */
Object evalMathExpr(Object anOR, JExprMath anExpr) throws Exception
{
    // Get first value
    JExprMath.Op op = anExpr.getOp(); int opCount = anExpr.getOperandCount();
    JExpr expr1 = anExpr.getOperand(0);
    Object val1 = evalExpr(anOR, expr1);
    
    // Handle Unary
    if(opCount==1) {
        if(op==JExprMath.Op.Not) {
            if(isBoolean(val1)) { boolean val = boolValue(val1);
                return mirrorOf(!val); }
            throw new RuntimeException("Logical Not MathExpr not boolean: " + anExpr.toString());
        }
        if(op==JExprMath.Op.Negate) { // Need to not promote everything to double
            if(isPrimitive(val1)) { double val = doubleValue(val1);
                return mirrorOf(-val); }
            throw new RuntimeException("Numeric Negate MathExpr not numeric: " + anExpr.toString());
        }
        else switch(op) {
            case Not:
            default: throw new RuntimeException("Operator not supported " + anExpr.getOp());
            //PreIncrement, PreDecrement, BitComp, PostIncrement, PostDecrement
        }
    }
    
    // Handle Binary
    else if(opCount==2) {
        JExpr expr2 = anExpr.getOperand(1);
        Object val2 = evalExpr(anOR, expr2);
        switch(op) {
            case Add: return add(val1, val2);
            case Subtract: return subtract(val1, val2);
            case Multiply: return multiply(val1, val2);
            case Divide: return divide(val1, val2);
            case Mod: return mod(val1, val2);
            case Equal: case NotEqual: case LessThan: case GreaterThan:
            case LessThanOrEqual: case GreaterThanOrEqual: return compareNumeric(val1, val2, op);
            case Or: case And: return compareLogical(val1, val2, op);
            default: throw new RuntimeException("Operator not supported " + anExpr.getOp());
            // BitOr, BitXOr, BitAnd, InstanceOf, ShiftLeft, ShiftRight, ShiftRightUnsigned,
        }
    }
    
    // Handle ternary
    else if(opCount==3 && op==JExprMath.Op.Conditional) {
        if(!isPrimitive(val1)) throw new RuntimeException("Ternary conditional expr not bool: " + expr1);
        boolean result = boolValue(val1);
        JExpr expr = result? anExpr.getOperand(1) : anExpr.getOperand(2);
        return evalExpr(anOR, expr);
    }
    
    // Complain
    throw new RuntimeException("Invalid MathExpr " + anExpr.toString());
}

/**
 * Add two values.
 */
Object add(Object aVal1, Object aVal2)
{
    if(isString(aVal1) || isString(aVal2))
        return mirrorOf(toString(aVal1) + toString(aVal2));
    if(isPrimitive(aVal1) && isPrimitive(aVal2)) {
        double result = doubleValue(aVal1) + doubleValue(aVal2);
        return value(result, aVal1, aVal2);
    }
    throw new RuntimeException("Can't add types " + aVal1 + " + " + aVal2);
}

/**
 * Subtract two values.
 */
Object subtract(Object aVal1, Object aVal2)
{
    if(isPrimitive(aVal1) && isPrimitive(aVal2)) {
        double result = doubleValue(aVal1) - doubleValue(aVal2);
        return value(result, aVal1, aVal2);
    }
    throw new RuntimeException("Can't subtract types " + aVal1 + " + " + aVal2);
}

/**
 * Multiply two values.
 */
Object multiply(Object aVal1, Object aVal2)
{
    if(isPrimitive(aVal1) && isPrimitive(aVal2)) {
        double result = doubleValue(aVal1) * doubleValue(aVal2);
        return value(result, aVal1, aVal2);
    }
    throw new RuntimeException("Can't multiply types " + aVal1 + " + " + aVal2);
}

/**
 * Divide two values.
 */
Object divide(Object aVal1, Object aVal2)
{
    if(isPrimitive(aVal1) && isPrimitive(aVal2)) {
        double result = doubleValue(aVal1) / doubleValue(aVal2);
        return value(result, aVal1, aVal2);
    }
    throw new RuntimeException("Can't divide types " + aVal1 + " + " + aVal2);
}

/**
 * Mod two values.
 */
Object mod(Object aVal1, Object aVal2)
{
    if(isPrimitive(aVal1) && isPrimitive(aVal2)) {
        double result = longValue(aVal1) % longValue(aVal2);
        return value(result, aVal1, aVal2);
    }
    throw new RuntimeException("Can't mod types " + aVal1 + " + " + aVal2);
}

/**
 * Compare two numeric values.
 */
Object compareNumeric(Object aVal1, Object aVal2, JExprMath.Op anOp)
{
    if(isPrimitive(aVal1) && isPrimitive(aVal2)) {
        double v1 = doubleValue(aVal1), v2 = doubleValue(aVal2);
        boolean val = compareNumeric(v1, v2, anOp);
        return mirrorOf(val);
    }
    throw new RuntimeException("Can't numeric compare types " + aVal1 + " + " + aVal2);
}

/**
 * Compare two numeric values.
 */
boolean compareNumeric(double aVal1, double aVal2, JExprMath.Op anOp)
{
    switch(anOp) {
        case Equal: return aVal1==aVal2; case NotEqual: return aVal1!=aVal2;
        case LessThan: return aVal1<aVal2; case GreaterThan: return aVal1>aVal2;
        case LessThanOrEqual: return aVal1<=aVal2; case GreaterThanOrEqual: return aVal1>=aVal2;
        default: throw new RuntimeException("Not a compare op " + anOp);
    }
}

/**
 * Compare two boolean values.
 */
Object compareLogical(Object aVal1, Object aVal2, JExprMath.Op anOp)
{
    if(isPrimitive(aVal1) && isPrimitive(aVal2)) {
        boolean v1 = boolValue(aVal1), v2 = boolValue(aVal2);
        boolean val = compareLogical(v1, v2, anOp);
        return mirrorOf(val);
    }
    throw new RuntimeException("Can't logical compare types " + aVal1 + " + " + aVal2);
}

/**
 * Compare two values.
 */
private static boolean compareLogical(boolean aVal1, boolean aVal2, JExprMath.Op anOp)
{
    if(anOp==JExprMath.Op.And) return aVal1 && aVal2;
    if(anOp==JExprMath.Op.Or) return aVal1 && aVal2;
    throw new RuntimeException("Not a compare op " + anOp);
}

/**
 * Return value of appropriate type for given number and original two values.
 */
Object value(double aValue, Object aVal1, Object aVal2)
{
    if(isDouble(aVal1) || isDouble(aVal2))
        return mirrorOf(aValue);
    if(isFloat(aVal1) || isFloat(aVal2))
        return mirrorOf((float)aValue);
    if(isLong(aVal1) || isLong(aVal2))
        return mirrorOf((long)aValue);
    if(isInt(aVal1) || isInt(aVal2))
        return mirrorOf((int)aValue);
    throw new RuntimeException("Can't discern value type for " + aVal1 + " and " + aVal2);
}

/**
 * Return the current this object.
 */
public Object thisObject()  { return _thisObj; }

/**
 * Return whether object is primitive.
 */
public boolean isPrimitive(Object anObj)  { return isInt(anObj) || isLong(anObj) || isFloat(anObj) || isDouble(anObj); }

/**
 * Return whether object is boolean.
 */
public boolean isBoolean(Object anObj)  { return anObj instanceof Boolean; }

/**
 * Return whether object is int.
 */
public boolean isInt(Object anObj)  { return anObj instanceof Integer; }

/**
 * Return whether object is long.
 */
public boolean isLong(Object anObj)  { return anObj instanceof Long; }

/**
 * Return whether object is float.
 */
public boolean isFloat(Object anObj)  { return anObj instanceof Float; }

/**
 * Return whether object is double.
 */
public boolean isDouble(Object anObj)  { return anObj instanceof Double; }

/**
 * Return whether object is String.
 */
public boolean isString(Object anObj)  { return anObj instanceof String; }

/**
 * Returns whether object is array value.
 */
public boolean isArray(Object anObj)  { return anObj!=null && anObj.getClass().isArray(); }

/**
 * Returns the boolean value.
 */
public boolean boolValue(Object anObj)  { return SnapUtils.boolValue(anObj); }

/**
 * Returns the int value.
 */
public int intValue(Object anObj)  { return SnapUtils.intValue(anObj); }

/**
 * Returns the long value.
 */
public long longValue(Object anObj)  { return SnapUtils.longValue(anObj); }

/**
 * Returns the float value.
 */
public float floatValue(Object anObj)  { return SnapUtils.floatValue(anObj); }

/**
 * Returns the double value.
 */
public double doubleValue(Object anObj)  { return SnapUtils.doubleValue(anObj); }

/**
 * Returns the array value at given index.
 */
public Object arrayValue(Object anObj, int anIndex)
{
    if(anObj instanceof Object[])
        return ((Object[])anObj)[anIndex];
    return null;
}

/**
 * Invoke method.
 */
public Object invokeMethod(Object anObj, String aName, List <Object> theArgs)
{
    return null;
}

/**
 * Return the current this object.
 */
public Object mirrorOf(Object anObj)  { return anObj; }

/**
 * Return the current this object.
 */
public String toString(Object anObj)  { return anObj!=null? anObj.toString() : null; }

/**
 * Returns a new evaluator for given object.
 */
public static EvalExpr get(Object anObj)
{
    EvalExpr eval = new EvalExpr(); eval._thisObj = anObj;
    return eval;
}

}