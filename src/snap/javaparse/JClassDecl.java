/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snap.javaparse;
import java.lang.reflect.Field;
import java.util.*;
import snap.util.*;

/**
 * A Java member for ClassDecl.
 */
public class JClassDecl extends JMemberDecl
{
    // The type of class (Class, Interface, Enum, Annotation)
    ClassType              _classType = ClassType.Class;
    
    // TypeParams
    List <JTypeParam>      _typeParams;
    
    // The extends list
    List <JType>           _extendsTypes = new ArrayList();
    
    // The implements list
    List <JType>           _implementsTypes = new ArrayList();
    
    // The list of fields, methods, enums annotations and child classes
    List <JMemberDecl>     _members = new ArrayList();
    
    // The enum constants (if ClassType Enum)
    List <JEnumConst>      _enumConstants = new ArrayList();
    
    // The field declarations
    JFieldDecl             _fieldDecls[];
    
    // The constructor declarations
    JConstrDecl            _constrDecls[];
    
    // The method declarations
    JMethodDecl            _methodDecls[];
    
    // An array of class declarations that are members of this class
    JClassDecl             _classDecls[];
    
    // The class type
    public enum ClassType { Class, Interface, Enum, Annotation }

/**
 * Returns the simple name.
 */
public String getSimpleName()  { return getName(); }

/**
 * Returns the JTypeParam(s).
 */
public List <JTypeParam> getTypeParams()  { return _typeParams; }

/**
 * Sets the JTypeParam(s).
 */
public void setTypeParams(List <JTypeParam> theTPs)
{
    if(_typeParams!=null) for(JNode n : _typeParams) removeChild(n);
    _typeParams = theTPs;
    if(_typeParams!=null) for(JNode n : _typeParams) addChild(n, -1);
}

/**
 * Returns the extends list.
 */
public List <JType> getExtendsTypes()  { return _extendsTypes; }

/**
 * Returns the extends list.
 */
public void addExtendsType(JType aType)  { _extendsTypes.add(aType); addChild(aType, -1); }

/**
 * Returns the implements list.
 */
public List <JType> getImplementsTypes()  { return _implementsTypes; }

/**
 * Returns the implements list.
 */
public void addImplementsType(JType aType)  { _implementsTypes.add(aType); addChild(aType, -1); }

/**
 * Returns the list of enum constants.
 */
public List <JEnumConst> getEnumConstants()  { return _enumConstants; }

/**
 * Adds an enum constant.
 */
public void addEnumConstant(JEnumConst anEC)  { _enumConstants.add(anEC); addChild(anEC, -1); }

/**
 * Returns the superclass.
 */
public Class getSuperClass()
{
    Class sc = _extendsTypes.size()>0? _extendsTypes.get(0).getJClass() : null;
    return sc!=null? sc : Object.class;
}

/**
 * Returns implemented interfaces.
 */
public Class[] getInterfaces()
{
    List <Class> classes = new ArrayList();
    for(JType interfType : _implementsTypes) {
        Class iclass = interfType.getJClass();
        if(iclass!=null)
            classes.add(iclass);
    }
    return classes.toArray(new Class[classes.size()]);
}

/**
 * Returns the class type.
 */
public ClassType getClassType()  { return _classType; }

/**
 * Sets the class type.
 */
public void setClassType(ClassType aType)  { _classType = aType; }

/**
 * Returns whether class type is Interface.
 */
public boolean isInterface()  { return getClassType()==ClassType.Interface; }

/**
 * Returns whether class is anonymous class.
 */
public boolean isAnonymousClass()  { return getId()==null; }

/**
 * Returns the list of member declarations.
 */
public List <JMemberDecl> getMemberDecls()  { return _members; }

/**
 * Adds a member declaration.
 */
public void addMemberDecl(JMemberDecl aDecl)  { _members.add(aDecl); addChild(aDecl, -1); }

/**
 * Sets the list of member declarations.
 */
public void setMemberDecls(List <JMemberDecl> theMDs)
{
    for(JMemberDecl md : _members) removeChild(md);
    _members = theMDs;
    for(JMemberDecl md : _members) addChild(md, -1);
}

/**
 * Returns the class field declarations.
 */
public JFieldDecl[] getFieldDecls()
{
    if(_fieldDecls!=null) return _fieldDecls;
    List <JFieldDecl> fds = new ArrayList();
    for(JMemberDecl member : _members)
        if(member instanceof JFieldDecl)
            fds.add((JFieldDecl)member);
    return _fieldDecls=fds.toArray(new JFieldDecl[fds.size()]);
}

/**
 * Returns the class constructor declarations.
 */
public JConstrDecl[] getConstructorDecls()
{
    if(_constrDecls!=null) return _constrDecls;
    List <JConstrDecl> cds = new ArrayList();
    for(JMemberDecl member : _members)
        if(member instanceof JConstrDecl)
            cds.add((JConstrDecl)member);
    return _constrDecls=cds.toArray(new JConstrDecl[cds.size()]);
}

/**
 * Returns the JConstructorDecl for given name.
 */
public JConstrDecl getConstructorDecl(String aName, Class theClasses[])
{
    for(JConstrDecl cd : getConstructorDecls())
        if(cd.getName().equals(aName))
            return cd;
    return null;
}

/**
 * Returns the class method declarations.
 */
public JMethodDecl[] getMethodDecls()
{
    if(_methodDecls!=null) return _methodDecls;
    List <JMethodDecl> mds = new ArrayList();
    for(JMemberDecl member : _members)
        if(member instanceof JMethodDecl && !(member instanceof JConstrDecl))
            mds.add((JMethodDecl)member);
    return _methodDecls=mds.toArray(new JMethodDecl[mds.size()]);
}

/**
 * Returns the JMethodDecl for given name.
 */
public JMethodDecl getMethodDecl(String aName, Class theClasses[])
{
    for(JMethodDecl md : getMethodDecls())
        if(md.getName().equals(aName))
            return md;
    return null;
}

/**
 * Returns the class constructor declarations.
 */
public JClassDecl[] getClassDecls()
{
    if(_classDecls!=null) return _classDecls;
    List <JClassDecl> cds = new ArrayList();
    for(JMemberDecl mbr : _members) getClassDecls(mbr, cds);
    return _classDecls=cds.toArray(new JClassDecl[cds.size()]); // Return class declarations
}

/**
 * Returns the class constructor declarations.
 */
private void getClassDecls(JNode aNode, List <JClassDecl> theCDs)
{
    if(aNode instanceof JClassDecl)
        theCDs.add((JClassDecl)aNode);
    else for(JNode c : aNode.getChildren())
        getClassDecls(c, theCDs);
}

/**
 * Returns the class declaration for class name.
 */
public JClassDecl getClassDecl(String aName)
{
    int index = aName.indexOf('.');
    String name = index>0? aName.substring(0, index) : aName;
    String remainder = index>=0? aName.substring(index+1) : null;
    for(JClassDecl cd : getClassDecls())
        if(cd.getSimpleName().equals(name))
            return remainder!=null? cd.getClassDecl(remainder) : cd;
    return null;
}

/**
 * Returns the simple name.
 */
protected String getNameImpl()
{
    // If identifier is available, just return name
    if(super.getNameImpl()!=null) return super.getNameImpl();
    
    // If enclosing class, see if we are inner class
    JClassDecl ecd = getEnclosingClassDecl();
    if(ecd!=null) {
        JClassDecl classDecls[] = ecd.getClassDecls();
        for(int i=0, iMax=classDecls.length, j=1; i<iMax; i++) { JClassDecl cd = classDecls[i];
            if(cd==this) return Integer.toString(j);
            if(cd.isAnonymousClass()) j++;
        }
    }
    
    // Return null since not found
    System.err.println("JClassDecl.createName: Name not found");
    return null;
}

/**
 * Returns the class declaration.
 */
protected JavaDecl getDeclImpl()
{
    // If enclosing class declaration, return ThatClassName$ThisName, otherwise return JFile.Name
    String cname = null;
    JClassDecl ecd = getEnclosingClassDecl();
    if(ecd!=null) {
        String ecname = ecd.getClassName(), name = getName();
        cname = ecname!=null && name!=null? ecname + '$' + name : null; }
    else cname = getFile().getName();
    
    // Return class name
    Class cls = cname!=null? getClassForName(cname) : null;
    try { return cls!=null? new JavaDecl(cls) : null; }
    catch(Throwable e) { System.err.println("JClassDecl.getDeclImpl: " + e); return null; }
}

/**
 * Override to check field declarations for id.
 */
@Override
protected JavaDecl resolveName(JNode aNode)
{
    // If class id, return class declaration
    if(aNode==_id)
        return getDecl();
    
    // If Extends or Implements node, forward on
    if(aNode.getParent()==this)
        return super.resolveName(aNode);
    
    // If it's "this", set class and return ClassField
    String name = aNode.getName(); boolean isId = aNode instanceof JExprId, isType = !isId;
    if(name.equals("this"))
        return getDecl();
    
    // If it's "super", set class and return ClassField
    if(name.equals("super")) {
        Class sclass = getSuperClass();
        return sclass!=null? new JavaDecl(sclass) : null;
    }
    
    // Iterate over fields and return declaration if found
    if(isId) for(JFieldDecl fd : getFieldDecls()) {
        for(JVarDecl vd : fd.getVarDecls())
            if(SnapUtils.equals(vd.getName(), name))
                return vd.getDecl(); }
    
    // See if it's a field reference from superclass
    Class sclass = getSuperClass();
    Field field = sclass!=null? ClassUtils.getField(sclass, name) : null;
    if(field!=null)
        return new JavaDecl(field);
    
    // Check interfaces
    if(isId) for(JType tp : getImplementsTypes()) {
        Class cls = tp.getJClass();
        Field fd = cls!=null? ClassUtils.getField(cls, name) : null;
        if(fd!=null)
            return new JavaDecl(fd);
    }

    // Look for JTypeParam for given name
    JTypeParam tp = getTypeParam(name);
    if(tp!=null)
        return tp.getDecl();
    
    // Look for InnerClass of given name
    Class cdClass = getJClass();
    Class cls = null; try { cls = cdClass!=null? ClassUtils.getClass(cdClass, name) : null; } catch(Throwable t) { }
    if(cls!=null)
        return new JavaDecl(cls);
    
    // Do normal version
    return super.resolveName(aNode);
}

/**
 * Returns a variable with given name.
 */
public List <JVarDecl> getVarDecls(String aPrefix, List <JVarDecl> theVDs)
{
    // Iterate over statements and see if any JStmtVarDecl contains variable with that name
    for(JMemberDecl m : _members) {
        if(m instanceof JFieldDecl) { JFieldDecl field = (JFieldDecl)m;
            for(JVarDecl v : field.getVarDecls())
                if(StringUtils.startsWithIC(v.getName(), aPrefix))
                    theVDs.add(v);
        }
    }
    
    // Do normal version
    return super.getVarDecls(aPrefix, theVDs);
}

/**
 * Returns the part name.
 */
public String getNodeString()
{
    switch(getClassType()) {
        case Interface: return "InterfaceDecl";
        case Enum: return "EnumDecl";
        case Annotation: return "AnnotationDecl";
        default: return "ClassDecl";
    }
}

}