package snap.typescript;

/**
 * A class to append TypeScript things.
 */
public class TypeBuffer {

    // The StringBuffer
    StringBuffer      _sb = new StringBuffer();

/**
 * Append indent.
 */
public TypeBuffer indent()  { _sb.append("    "); return this; }

/**
 * Append String.
 */
public TypeBuffer append(String aStr)  { _sb.append(aStr); return this; }

/**
 * Append char.
 */
public TypeBuffer append(char aValue)  { _sb.append(aValue); return this; }

/**
 * Append Int.
 */
public TypeBuffer append(int aValue)  { _sb.append(aValue); return this; }

/**
 * Append Double.
 */
public TypeBuffer append(double aValue)  { _sb.append(aValue); return this; }

/**
 * Append newline.
 */
public TypeBuffer endln()  { _sb.append('\n'); return this; }
}