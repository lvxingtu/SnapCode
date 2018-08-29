package snap.javascript;
import java.util.*;
import snap.javakit.*;

/**
 * A custom class.
 */
public class JSWriterUtils {
    
/**
 * Returns the method name for given JExpr
 */
public static String getMethodName(JExprMethodCall aExpr)
{
    String name = aExpr.getId().getName();
    if(name.equals("add")) {
        if(aExpr.getDecl().getClassName().equals("java.util.List"))
            name = "push";
    }
    return name;
}

/**
 * Returns a type string.
 */
public static String getTypeString(JType aType)
{
    // Get name
    String name = aType.getName();
    String cname = aType.getEvalClassName();
    
    // Map core TypeScript types
    if(name.equals("void")) name = "";
    else if(name.equals("Object")) name = "any";
    else if(name.equals("String") || name.equals("char") || name.equals("Character")) name = "string";
    else if(aType.isNumberType()) name = "number";
    else if(name.equals("Boolean")) name = "boolean";
    else if(name.equals("ArrayList")) name = "Array";
    //else if(cname!=null && cname.startsWith("java.lang.")) name = "java.lang." + name;
    
    // Add array chars
    if(aType.isArrayType())
        name = name + "[]";
        
    // If enum, add class name
    Class cls = aType.getEvalClass();
    if(cls!=null && cls.isEnum()) {
        Class ecls = cls.getEnclosingClass();
        if(ecls!=null) name = ecls.getSimpleName() + '.' + name;
    }
    
    // Return name
    return name;
}

/**
 * Returns a type string.
 */
public static String getTypeStringShort(JType aType)
{
    // Map core TypeScript types
    String name = aType.getName();
    if(name.equals("void")) name = "";
    else if(name.equals("Object")) name = "any";
    else if(name.equals("String") || name.equals("char") || name.equals("Character")) name = "str";
    else if(aType.isNumberType()) name = "num";
    else if(name.equals("Boolean")) name = "bool";
    
    // Add array chars
    if(aType.isArrayType())
        name = name + "_ary";

    // Return name
    return name;
}

/**
 * Returns a name for given method declaration, based on types.
 */
public static String getMethodNameUnique(JMethodDecl aMDecl)
{
    String name = aMDecl.getName() + '_';
    List <JVarDecl> params = aMDecl.getParameters();
    JVarDecl plast = params.size()>0? params.get(params.size()-1) : null;
    for(JVarDecl vd : params) {
        name += getTypeStringShort(vd.getType()); if(vd!=plast) name += '_'; }
    return name;
}

/**
 * Returns a common return type for given array of method declarations.
 */
public static JType getCommonReturnType(JMethodDecl theMDecls[])
{
    JType type = null;
    for(JMethodDecl md : theMDecls)
        type = getCommonType(type, md.getType());
    return type;
}

/**
 * Returns the array of common parameter types for given array of method declarations.
 */
public static JVarDecl[] getCommonParams(JMethodDecl theMDecls[])
{
    // Iterate over method decls to get common names and parameter types
    List <String> pnames = new ArrayList();
    List <JType> ptypes = new ArrayList();
    for(JMethodDecl md : theMDecls) {
        
        // Iterate over method decl params to get common names and param types
        List <JVarDecl> params = md.getParameters();
        for(int i=0,iMax=params.size(); i<iMax; i++) { JVarDecl param = params.get(i);
            String name = param.getName();
            JType type = param.getType();
            
            // If param types list already has a condidate in this position, get common type/name
            if(i<ptypes.size()) {
                ptypes.set(i, getCommonType(ptypes.get(i), type));
                if(!name.equals(pnames.get(i)) && !pnames.get(i).startsWith("arg"))
                    pnames.set(i, "arg" + i);
            }
            
            // If no cadidate yet for this position, just add type & name
            else { ptypes.add(type); pnames.add(name); }
        }
    }
    
    // Iterate over ptypes/pnames, create JVarDecl array and return
    JVarDecl vdecls[] = new JVarDecl[ptypes.size()];
    for(int i=0,iMax=ptypes.size(); i<iMax; i++) {
        JVarDecl vd = new JVarDecl();
        vd.setId(new JExprId(pnames.get(i)));
        vd.setType(ptypes.get(i)); vdecls[i] = vd;
        vd.setParent(theMDecls[0]);
    }
    return vdecls;
}

/**
 * Returns a common return type for given array of method delclarations.
 */
public static JType getCommonType(JType aTyp0, JType aTyp1)
{
    // If either is null, return other
    if(aTyp0==null || aTyp1==null) return aTyp0!=null? aTyp0 : aTyp1;
    
    // If either is void, return other
    String tstr0 = getTypeString(aTyp0), tstr1 = getTypeString(aTyp1);
    if(tstr0.length()==0 || tstr1.length()==0) return tstr0.length()==0? aTyp1 : aTyp0;
    
    // If types are equal return type 0
    if(tstr0.equals(tstr1)) return aTyp0;
    
    // Return Object
    if(tstr0.equals("Object")) return aTyp0;
    if(tstr1.equals("Object")) return aTyp1;
    JType type = new JType(); type.setName("Object");
    type.setParent(aTyp0.getParent());
    return type;
}

/**
 * Sorts an array of JMethodDecl by arg count.
 */
public static void sort(JMethodDecl theMDecls[])
{
    Arrays.sort(theMDecls, (o1,o2) ->
        o1.getParamCount()<o2.getParamCount()? -1 : o1.getParamCount()>o2.getParamCount()? 1 :0);
}

/**
 * Returns the conditionals to specify a specific method decl: if(args-are-correct) return call-method-decl-X;
 */
public static List <String> getMethodDispatchConditionals(JMethodDecl theMDecls[], JVarDecl theParams[], boolean doRtrn)
{
    // Iterate over method declarations and create conditional line for each
    List <String> lines = new ArrayList();
    for(int i=theMDecls.length-1;i>=0;i--) { JMethodDecl md = theMDecls[i]; int pcount = md.getParamCount();
    
        // Create string buffer with start of conditional
        StringBuffer sb = new StringBuffer("if(");
        
        // Iterate over args and build arg check: if((argX != null && argX instanceof type) || argX === null) && ...
        //                 or for primitive type: if((typeof argX ==='ptype') || argX ===nul) && ...
        for(int j=0;j<pcount;j++) { JVarDecl vd = md.getParam(j); String arg = theParams[j].getName();
            String tstr = getTypeString(vd.getType()); if(tstr.contains("[]")) tstr = "Array";
            boolean prim = tstr.equals("boolean") || tstr.equals("number") || tstr.equals("string");
            if(prim) {
                sb.append("((typeof ").append(arg).append(" === ").append('\'').append(tstr).append("\')");
                sb.append(" || ");
            }
            else {
                sb.append("((").append(arg).append(" != null && ");
                sb.append(arg).append(" instanceof ").append(tstr).append(") || ");
            }
            sb.append(arg).append(" === null)");
            if(j+1<pcount) sb.append(" && ");
        }
        
        // If remaining args, add check for undefined
        if(pcount<theParams.length)
            sb.append(pcount>0? " && " : "").append(theParams[pcount].getName()).append(" === undefined");
        
        // Close conditional
        sb.append(')');
        lines.add(sb.toString());
        
        // If doReturns
        if(doRtrn) {
            sb.setLength(0); sb.append("    ");
            if(!md.getType().getName().equals("void")) sb.append(" return");
            sb.append(" this.").append(getMethodNameUnique(md)).append('(');
            for(int j=0;j<pcount;j++) { JVarDecl p = theParams[j];
                sb.append(p.getName()); if(j+1<pcount) sb.append(", "); }
            sb.append(");");
            lines.add(sb.toString());
        }
    }
    return lines;
}

}