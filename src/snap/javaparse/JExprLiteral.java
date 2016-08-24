package snap.javaparse;

/**
 * A JExpr subclass for literals.
 */
public class JExprLiteral extends JExpr {

    // The string used to represent literal
    String        _valueString;

    // The literal value
    Object        _value;

    // The literal type
    LiteralType   _literalType = LiteralType.Null;

    // Constants for Literal type
    public enum LiteralType { Boolean, Integer, Long, Float, Double, Character, String, Null };

/**
 * Creates a new literal.
 */
public JExprLiteral()  { }

/**
 * Creates a new literal with given value.
 */
public JExprLiteral(Object aValue)
{
    _value = aValue;
    if(aValue instanceof Boolean) _literalType = LiteralType.Boolean;
    else if(aValue instanceof Integer) _literalType = LiteralType.Integer;
    else if(aValue instanceof Long) _literalType = LiteralType.Long;
    else if(aValue instanceof Float) _literalType = LiteralType.Float;
    else if(aValue instanceof Double) _literalType = LiteralType.Double;
    else if(aValue instanceof Character) _literalType = LiteralType.Character;
    else if(aValue instanceof String) _literalType = LiteralType.String;
    else if(aValue==null) _literalType = LiteralType.Null;
    _valueString = aValue.toString();
}

/**
 * Returns the Literal type (String, Number, Boolean, Null).
 */
public LiteralType getLiteralType()  { return _literalType; }

/**
 * Sets the literal type.
 */
public void setLiteralType(LiteralType aType)  { _literalType = aType; }

/**
 * Returns whether this is null literal.
 */
public boolean isNull()  { return getLiteralType()==LiteralType.Null; }

/**
 * Returns the value.
 */
public Object getValue()  { return _value!=null || isNull()? _value : (_value=createValue()); }

/**
 * Creates the value.
 */
protected Object createValue()
{
    // Get literal type and string info
    String s = getValueString(); int len = s.length(); char c = s.charAt(len-1);
    
    // Decode type from string
    switch(getLiteralType()) {
        case Boolean: return Boolean.valueOf(s);
        case Integer: try { return Integer.decode(s); } catch(Exception e) { return 0; }
        case Long: try { return Long.decode(s.substring(0,len-1)); } catch(Exception e) { return 0; }
        case Float: return Float.valueOf(s.substring(0,len-1));
        case Double: return c=='d' || c=='D'? Double.valueOf(s.substring(0,len-1)) : Double.valueOf(s);
        case Character: return s.charAt(1);
        case String: return s.substring(1, s.length()-1);
        default: return null;
    }
}

/**
 * Returns the value string.
 */
public String getValueString()  { return _valueString; }

/**
 * Sets the value string.
 */
public void setValueString(String aString)  { _valueString = aString; }

/**
 * Tries to resolve the class declaration for this node.
 */
protected JavaDecl getDeclImpl()
{
    Class cls = getValue()!=null? getValue().getClass() : null;
    return cls!=null? new JavaDecl(cls) : null;
}
        
/**
 * Returns the part name.
 */
public String getNodeString()
{
    switch(getLiteralType()) {
        case Boolean: return "Boolean";
        case Integer: return "Integer";
        case Long: return "Long";
        case Float: return "Float";
        case Double: return "Double";
        case Character: return "Character";
        case String: return "String";
        case Null: return "Null";
        default: throw new RuntimeException("JLiteral unknown type: " + getLiteralType());
    }
}

}