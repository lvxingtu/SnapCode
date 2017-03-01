/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snap.javaparse;
import java.util.*;
import snap.util.ClassUtils;
import snap.util.SnapUtils;

/**
 * A JNode for types.
 */
public class JType extends JNode {

    // Whether type is primitive type
    boolean               _primitive;
    
    // Whether is reference (array or class/interface type)
    int                   _arrayCount;
    
    // The generic Types
    List <JType>          _typeArgs;
    
    // The base type
    JavaDecl              _baseDecl;
    
/**
 * Sets the type name.
 */
public void setName(String aName)  { _name = aName; }

/**
 * Returns the simple name.
 */
public String getSimpleName()
{
    int index = _name.lastIndexOf('.');
    return index>0?  _name.substring(index+1, _name.length()) : _name;
}

/**
 * Returns whether type is primitive type.
 */
public boolean isPrimitive()  { return _primitive; }

/**
 * Sets whether type is primitive type.
 */
public void setPrimitive(boolean aValue)  { _primitive = aValue; }

/**
 * Returns whether type is array.
 */
public boolean isArrayType()  { return _arrayCount>0; }

/**
 * Returns the array count if array type.
 */
public int getArrayCount()  { return _arrayCount; }

/**
 * Sets the array count.
 */
public void setArrayCount(int aValue)  { _arrayCount = aValue; }

/**
 * Returns whether type is reference (array or class/interface type).
 */
public boolean isReferenceType()  { return _primitive && _arrayCount==0; }

/**
 * Override to resolve type class name and create declaration from that.
 */
protected JavaDecl getDeclImpl()
{
    // Get base decl
    JavaDecl decl = getBaseDecl();

    // Handle array
    if(_arrayCount>0 && decl!=null) {
        String cname = decl.getClassName();
        String pfix = "["; for(int i=1; i<_arrayCount; i++) pfix += "[";
        if(_primitive) cname = pfix + ClassUtils.getClassCoded(cname);
        else cname = pfix + "L" + cname + ";";
        decl = new JavaDecl(cname, null, null, null);
    }
    
    // Return declaration
    return decl;
}

/**
 * Override to resolve type class name and create declaration from that.
 */
protected JavaDecl getBaseDecl()
{
    // If already set, just return
    if(_baseDecl!=null) return _baseDecl;
    
    // Handle primitive type
    JavaDecl decl = null;
    Class pclass = ClassUtils.getPrimitiveClass(_name);
    if(pclass!=null)
        decl = new JavaDecl(pclass);
    
    // If not primitive, try to resolve
    if(decl==null)
        decl = resolveName(this);

    // Return declaration
    //if(decl==null) System.err.println("Unresolved Type: " + this);
    return _baseDecl = decl;
}

/**
 * Returns the generic types.
 */
public List <JType> getTypeArgs()  { return _typeArgs; }

/**
 * Adds a type arg.
 */
public void addTypeArg(JType aType)
{
    if(_typeArgs==null) _typeArgs = new ArrayList();
    _typeArgs.add(aType); addChild(aType, -1);
}

    
/**
 * Returns the Java code string for node.
 */
protected void append(StringBuffer aSB)
{
    aSB.append(getName());
    for(int i=0;i<getArrayCount();i++) aSB.append("[]");
}

/**
 * Returns whether type is number type.
 */
public boolean isNumberType()
{
    JavaDecl decl = getBaseDecl();
    String tp = decl!=null? decl.getClassName() : null; if(tp==null) return false;
    tp = tp.intern();
    return tp=="byte" || tp=="short" || tp=="int" || tp=="long" || tp=="float" || tp=="double" ||
        tp=="java.lang.Byte" || tp=="java.lang.Short" || tp=="java.lang.Integer" || tp=="java.lang.Long" ||
        tp=="java.lang.Float" || tp=="java.lang.Double" || tp=="java.lang.Number";
}

/**
 * Standard equals implementation.
 */
public boolean equals(Object anObj)
{
    // Check identity and get other JType
    if(anObj==this) return true;
    JType other = anObj instanceof JType? (JType)anObj : null; if(other==null) return false;
    
    // If simple names don't match, return false
    if(!other.getSimpleName().equals(getSimpleName())) return false;

    // Check Class names
    String cn1 = getClassName(), cn2 = other.getClassName(); if(cn1!=null && cn2!=null) return cn1.equals(cn2);
    return SnapUtils.equals(_name, other._name); // Check name
}

/**
 * Standard hashCode implementation.
 */
public int hashCode()  { return _name!=null? _name.hashCode() : 0; }

}