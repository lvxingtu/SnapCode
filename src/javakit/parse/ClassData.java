/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.parse;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import snap.project.Project;
import snap.util.ClassUtils;
import snap.web.*;

/**
 * A file to represent a Java class.
 */
public class ClassData {

    // The class file
    WebFile        _file;
    
    // The project
    Project        _proj;
    
    // The class name
    String         _cname;

/**
 * Creates a new ClassData for given file.
 */
public ClassData(WebFile aFile)  { _file = aFile; _proj = Project.get(_file); }

/**
 * Returns the set of JavaDecls that this class references.
 */
public void getRefs(Set <JavaDecl> theRefs)
{
    // Get bytes
    if(_file.getBytes()==null) return;
    
    // Get ClassFile reader and read
    ClassFileData cfd = new ClassFileData();
    try { cfd.read(new DataInputStream(_file.getInputStream())); }
    catch(Exception e) { System.err.println(e); return; }
    
    // Iterate over constants and add to set top level class names
    String cname = _cname = getRootClassName(_proj.getClassName(_file));
    for(int i=1, iMax=cfd.getConstantCount(); i<=iMax; i++) { ClassFileData.Constant constant = cfd.getConstant(i);
        if(constant.isClass() && (isInRootClassName(cname, constant.getClassName()) ||
            ClassUtils.isPrimitiveClassName(constant.getClassName())))
            continue;
        JavaDecl ref = getRef(constant);
        if(ref!=null) theRefs.add(ref);
    }
    
    // Get class and make sure TypeParameters, superclass and interfaces are in refs
    Class cls = _proj.getClassForFile(_file);
    for(TypeVariable tp : cls.getTypeParameters()) addClassRef(tp, theRefs);
    addClassRef(cls.getGenericSuperclass(), theRefs);
    for(Type tp : cls.getGenericInterfaces()) addClassRef(tp, theRefs);
    
    // Fields: add JavaDecl for each declared field - also make sure field type is in refs
    Field fields[]; try { fields = cls.getDeclaredFields(); }
    catch(Throwable e) { System.err.println(e + " in " + _file); return; }
    for(Field field : fields)
        addClassRef(field.getGenericType(), theRefs);

    // Constructors: Add JavaDecl for each constructor - also make sure parameter types are in refs
    Constructor constrs[]; try { constrs = cls.getDeclaredConstructors(); }
    catch(Throwable e) { System.err.println(e + " in " + _file); return; }
    for(Constructor constr : constrs) {
        if(constr.isSynthetic()) continue;
        for(Type t : constr.getGenericParameterTypes()) addClassRef(t, theRefs);
    }
    
    // Methods: Add JavaDecl for each declared method - also make sure return/parameter types are in refs
    Method methods[]; try { methods = cls.getDeclaredMethods(); }
    catch(Throwable e) { System.err.println(e + " in " + _file); return; }
    for(Method meth : methods) {
        if(meth.isSynthetic()) continue;
        addClassRef(meth.getGenericReturnType(), theRefs);
        for(Type t : meth.getGenericParameterTypes()) addClassRef(t, theRefs);
    }
}

/**
 * Returns the JavaDecl for given Class ConstantPool Constant if external reference.
 */
private JavaDecl getRef(ClassFileData.Constant aConst)
{
    // Handle Class reference
    if(aConst.isClass()) {
        String cname = aConst.getClassName(); if(cname.startsWith("[")) return null;
        return _proj.getJavaDecl(cname);
    }
    
    // Handle Field reference
    if(aConst.isField()) {
        String cname = aConst.getDeclClassName(); if(cname.startsWith("[")) return null;
        JavaDeclClass cdecl = _proj.getClassDecl(cname);
        String name = aConst.getMemberName();
        return cdecl.getFieldDeep(name);
    }
    
    // Handle method reference
    if(aConst.isConstructor()) {
        
        // Get declaring class name
        String cname = aConst.getDeclClassName(); if(cname.startsWith("[")) return null;
        
        // Get parameter type names and JavaDecl array
        String pnames[] = aConst.getParameterTypes();
        JavaDecl ptypes[] = new JavaDecl[pnames.length];
        for(int i=0;i<pnames.length;i++) ptypes[i] = _proj.getJavaDecl(pnames[i]);
        
        // Get class decls and constructor from parameters
        JavaDeclClass cdecl = _proj.getClassDecl(cname);
        return cdecl.getConstructorDeclDeep(ptypes);
    }
    
    // Handle method reference
    if(aConst.isMethod()) {
        
        // Get declaring class name and method name
        String cname = aConst.getDeclClassName(); if(cname.startsWith("[")) return null;
        String name = aConst.getMemberName();
        
        // Get parameter type names and JavaDecl array
        String pnames[] = aConst.getParameterTypes();
        JavaDecl ptypes[] = new JavaDecl[pnames.length];
        for(int i=0;i<pnames.length;i++) ptypes[i] = _proj.getJavaDecl(pnames[i]);
        
        // Get class decls and method from name and parameters
        JavaDeclClass cdecl = _proj.getClassDecl(cname);
        return cdecl.getMethodDeclDeep(name, ptypes);
    }
    
    // Return null since unknown Constant reference
    return null;
}

/**
 * Adds a ref for a declaration type class.
 */
private final void addClassRef(Type aType, Set <JavaDecl> theRefs)
{
    // Handle simple Class
    if(aType instanceof Class) { Class cls = (Class)aType;
        while(cls.isArray()) cls = cls.getComponentType();
        if(cls.isAnonymousClass() || cls.isPrimitive() || cls.isSynthetic()) return;
        JavaDecl decl = _proj.getJavaDecl(cls);
        theRefs.add(decl);
    }
        
    // Handle ParameterizedType
    else if(aType instanceof ParameterizedType) { ParameterizedType ptype = (ParameterizedType)aType;
        addClassRef(ptype.getRawType(), theRefs);
        for(Type type : ptype.getActualTypeArguments())
            addClassRef(type, theRefs);
    }
    
    // Handle TypeVariable
    else if(aType instanceof TypeVariable) { TypeVariable tv = (TypeVariable)aType;
        for(Type type : tv.getBounds())
            if(type instanceof Class)  // Bogus!
                addClassRef(type, theRefs); }
        
    // Handle WildcardType
    else if(aType instanceof WildcardType) { WildcardType wct = (WildcardType)aType;
        for(Type type : wct.getLowerBounds())
            addClassRef(type, theRefs);
        for(Type type : wct.getUpperBounds())
            addClassRef(type, theRefs);
    }
}

/** Returns the top level class name. */
private static String getRootClassName(String cname)
{
    int i = cname.indexOf('$'); if(i>0) cname = cname.substring(0,i); return cname;
}

/** Returns a simple class name. */
private static boolean isInRootClassName(String aRoot, String aChild)
{
    return aChild.startsWith(aRoot) && (aChild.length()==aRoot.length() || aChild.charAt(aRoot.length())=='$');
}

/**
 * Returns the ClassData for given file.
 */
public static ClassData get(WebFile aFile)
{
    ClassData data = (ClassData)aFile.getProp(ClassData.class.getName());
    if(data==null) aFile.setProp(ClassData.class.getName(), data = new ClassData(aFile));
    return data;
}

}