/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snap.javakit;
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
    
/**
 * Returns the modifiers.
 */
public JModifiers getMods()
{
    if(_mods==null) setMods(new JModifiers());
    return _mods;
}

/**
 * Sets the modifiers.
 */
public void setMods(JModifiers aValue)
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
    replaceChild(_id, _id = anId);
    if(_id!=null) setName(_id.getName());
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
    JavaDecl decl = getDecl();
    return decl!=null && (decl.isMethod() || decl.isConstructor())? decl.getSuper() : null;
}

/**
 * Returns whether super declaration is interface.
 */
public boolean isSuperDeclInterface()
{
    JavaDecl sdecl = getSuperDecl(); if(sdecl==null) return false;
    JavaDecl cdecl = sdecl.getClassType(); if(cdecl==null) return false;
    return cdecl.isInterface();
}

}