package snap.typescript;

/**
 * A class to append TypeScript things.
 */
public class TypeBuffer {

    // The StringBuffer
    StringBuffer      _sb = new StringBuffer();
    
    // The indent string
    String            _indentStr = "    ";
    
    // The indent level
    int               _indent;
    
    // Whether at line end
    boolean           _lineStart;

/**
 * Append indent.
 */
public TypeBuffer indent()  { _indent++; return this; }

/**
 * Append indent.
 */
public TypeBuffer outdent()  { _indent--; return this; }

/**
 * Checks for indent.
 */
protected void cd()
{
    if(_lineStart)
        for(int i=0;i<_indent;i++)
            _sb.append(_indentStr);
    _lineStart = false;
}

/**
 * Append String.
 */
public TypeBuffer append(String aStr)  { cd(); _sb.append(aStr); return this; }

/**
 * Append char.
 */
public TypeBuffer append(char aValue)  { cd(); _sb.append(aValue); return this; }

/**
 * Append Int.
 */
public TypeBuffer append(int aValue)  { cd(); _sb.append(aValue); return this; }

/**
 * Append Double.
 */
public TypeBuffer append(double aValue)  { cd(); _sb.append(aValue); return this; }

/**
 * Append newline.
 */
public TypeBuffer endln()  { _sb.append('\n'); _lineStart = true; return this; }
}