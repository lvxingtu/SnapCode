/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snap.javaparse;
import java.util.List;

/**
 * A JNode for type members: Initializer, TypeDecl, EnumDecl, ConstrDecl, FieldDecl, MedthodDecl, AnnotationDecl.
 * For JavaParseRule: ClassBodyDecl.
 */
public class JMemberDecl extends JNode {

    // The modifiers
    JModifiers      _mods;
    
    // The name identifier
    JExprId         _id;
    
    // The member that this member overrides or implements
    JavaDecl        _super;
    
    // Whether super is null
    boolean         _superIsNull;
    
/**
 * Returns the member name.
 */
protected String getNameImpl()  { return _id!=null? _id.getName() : null; }

/**
 * Returns the modifiers.
 */
public JModifiers getModifiers()
{
    if(_mods==null) setModifiers(new JModifiers());
    return _mods;
}

/**
 * Sets the modifiers.
 */
public void setModifiers(JModifiers aValue)
{
    if(_mods==null) addChild(_mods=aValue);
    else replaceChild(_mods, _mods=aValue);
}

/**
 * Returns the identifier.
 */
public JExprId getId()  { return _id; }

/**
 * Sets the identifier.
 */
public void setId(JExprId anId)
{
    //if(_id==null) addChild(_id=anId, 0); else
    replaceChild(_id, _id = anId);
}

/**
 * Returns the type parameters for this member.
 */
public List <JTypeParam> getTypeParams()  { return null; }

/**
 * Returns the type parameters for this member.
 */
public JTypeParam getTypeParam(String aName)
{
    List <JTypeParam> tps = getTypeParams(); if(tps==null) return null;
    for(JTypeParam tp : tps)
        if(tp.getName().equals(aName))
            return tp;
    return null;
}

/**
 * Returns the member that this member overrides or implements, if available.
 */
public JavaDecl getSuperDecl()
{
    if(_super!=null || _superIsNull) return _super;
    _super = getSuperDeclImpl(); _superIsNull = _super==null;
    return _super;
}

/**
 * Returns the member that this member overrides or implements, if available.
 */
protected JavaDecl getSuperDeclImpl()  { return null; }

/**
 * Returns whether super declaration is interface.
 */
public boolean isSuperDeclInterface()
{
    JavaDecl sdecl = getSuperDecl(); if(sdecl==null) return false;
    Class cls = getClassForName(sdecl.getClassName());
    return cls!=null && cls.isInterface();
}

}