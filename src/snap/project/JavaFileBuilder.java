/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snap.project;
import java.util.*;
import snap.javaparse.*;
import snap.util.*;
import snap.web.WebFile;

/**
 * A FileBuilder to build Java files.
 */
public class JavaFileBuilder implements ProjectFileBuilder {

    // The project we work for
    Project                  _proj;

    // A list of files to be compiled
    Set <WebFile>            _buildFiles = Collections.synchronizedSet(new HashSet());
    
    // Whether to interrupt current build
    boolean                  _interrupt;
    
    // The SnapCompiler used for last compiles
    SnapCompiler             _compiler;
    
    // The final set of compiled files
    Set <WebFile>            _compiledFiles, _errorFiles;
    
/**
 * Creates a new JavaFileBuilder for given Project.
 */
public JavaFileBuilder(Project aProject)  { _proj = aProject; }

/**
 * Returns whether file is build file.
 */
public boolean isBuildFile(WebFile aFile)  { return aFile.getType().equals("java"); }

/**
 * Returns whether given file needs to be built.
 */
public boolean getNeedsBuild(WebFile aFile)
{
    // See if Java file has out of date Class file 
    WebFile cfile = _proj.getClassFile(aFile);
    boolean needsBuild = !cfile.getExists() || cfile.getLastModifiedTime()<aFile.getLastModifiedTime();
    
    // If not out of date, updateDependencies, compatibilities
    if(!needsBuild && !JavaData.get(aFile).isDependenciesSet()) {
        JavaData.get(aFile).updateDependencies(); needsBuild = true;
        //int c = updateCompatability(aFile); if(c<0) needsBuild=true; if(c!=-2) jdata.updateDependencies();
    }
    
    // Return NeedsBuild
    return needsBuild;
}

/**
 * Adds a compile file.
 */
public void addBuildFile(WebFile aFile)  { _buildFiles.add(aFile); }

/**
 * Remove a build file.
 */
public void removeBuildFile(WebFile aFile)
{
    // Remove from build files
    _buildFiles.remove(aFile);
    
    // Get dependent files and add to BuildFiles
    JavaData jdata = JavaData.get(aFile);
    Set <WebFile> dependents = jdata.getDependents();
    for(WebFile dependant : dependents)
        if(dependant.getExists())
            addBuildFile(dependant);
    
    // Remove JavaFile Dependencies
    jdata.removeDependencies();
    
    // Get JavaFile.ClassFiles and remove them
    WebFile cfiles[] = _proj.getClassFiles(aFile); if(cfiles==null) return;
    for(WebFile cfile : cfiles)
        try { cfile.delete(); }
        catch(Exception e) { throw new RuntimeException(e); }
}

/**
 * Compiles files.
 */
public boolean buildFiles(TaskMonitor aTaskMonitor)
{
    if(_buildFiles.size()==0) return true;
    List <WebFile> files = new ArrayList(_buildFiles); _buildFiles.clear();
    SnapCompiler compiler = new SnapCompiler(_proj);
    Set <WebFile> compiledFiles = new HashSet(), errorFiles = new HashSet();
    
    // Add known build files to FileManager, to force use of JavaFile instead of ClassFile, regardless of ModTime
    compiler.getFileManaer()._buildFiles.addAll(files);
    _interrupt = false;
    
    // Sort build files by dependencies
    if(files.size()>20) sortBuildFiles(files);
    
    // Iterate over build files and compile
    boolean compileSuccess = true; //long time = System.currentTimeMillis();
    for(int i=0; i<files.size(); i++) { WebFile file = files.get(i);
    
        // If interrupted, add remaining build files and return
        if(_interrupt) {
            for(int j=i, jMax=files.size(); j<jMax; j++) addBuildFile(files.get(j)); return false; }
        
        // Update progress
        if(compiledFiles.contains(file)) continue; //System.err.println("Skipping " + finfo);
        int count = compiledFiles.size() + 1;
        String msg = String.format("Compiling %s (%d of %d)", _proj.getClassName(file), count, files.size());
        aTaskMonitor.beginTask(msg, -1);
        
        // Get compile file
        boolean result = compiler.compile(file);
        aTaskMonitor.endTask();
        
        // If compile failed, re-add file to BuildFiles and continue
        if(!result) {
            compiledFiles.add(file); errorFiles.add(file);
            addBuildFile(file);
            if(compiler._errorCount>=1000) _interrupt = true;
            compileSuccess = false; continue;
        }
        
        // Add Compiler.CompiledFiles to CompiledFiles
        compiledFiles.addAll(compiler.getCompiledJavaFiles());
        
        // If there were modified files, clear Project.ClassLoader
        if(compiler.getModifiedJavaFiles().size()>0)
            _proj.clearClassLoader();
        
        // Iterate over JavaFiles for modified ClassFiles and update
        for(WebFile jfile : compiler.getModifiedJavaFiles()) {
            
            // Delete class files for removed inner classes
            deleteZombieClassFiles(jfile);
            
            // Update dependencies and get files that need to be updated
            Set <WebFile> updateFiles = JavaData.get(jfile).updateDependencies();
            for(WebFile ufile : updateFiles) {
                Project proj = Project.get(ufile);
                if(proj==_proj) {
                    if(!compiledFiles.contains(ufile) && !ListUtils.containsId(files, ufile))
                        files.add(ufile); }
                else proj.addBuildFileForce(ufile);
            }
        }
    }
    
    // Finalize TaskMonitor
    aTaskMonitor.beginTask("Build Completed", -1); aTaskMonitor.endTask();
    
    // Set compiler/files for findUnusedImports
    _compiler = compiler; _compiledFiles = compiledFiles; _errorFiles = errorFiles;
    
    // Finalize ActivityText and return
    //System.out.println("Build time: " + (System.currentTimeMillis()-time)/1000f + " seconds");
    return compileSuccess;
}

/**
 * Checks last set of compiled files for unused imports.
 */
public void findUnusedImports()
{
    if(_compiler==null) return;
    for(WebFile cfile : _compiledFiles) { JavaData jdata = JavaData.get(cfile);
        if(_errorFiles.contains(cfile)) continue;
        for(BuildIssue bissue : jdata.getUnusedImports())
            _compiler.report(bissue); }
    _compiler = null; _compiledFiles = _errorFiles = null;
}

/**
 * Delete inner-class class files that were generated in older version of class.
 */
private void deleteZombieClassFiles(WebFile aJavaFile)
{
    // Get all ClassFiles for JavaFile and delete those older than JavaFile
    WebFile cfiles[] = _proj.getClassFiles(aJavaFile); if(cfiles==null) return;
    for(WebFile cfile : cfiles) {
        if(cfile.getLastModifiedTime()<aJavaFile.getLastModifiedTime()) {
            try { cfile.delete(); }
            catch(Exception e) { throw new RuntimeException(e); }
        }
    }
}

/**
 * Does a Topological Sort of given project files by their dependencies.
 */
public void sortBuildFiles(List <WebFile> theItems)
{
    // S <- Set of all nodes with no incoming edges
    Set <Node> S = new HashSet<Node>();
    
    // Load all nodes
    Map <WebFile,Node> allNodesMap = new HashMap();
    for(WebFile file : theItems) {
        Node n = allNodesMap.get(file); if(n==null) allNodesMap.put(file, n=new Node(file));
        Set <WebFile> dps = JavaData.get(file).getDependencies();
        for(WebFile dpfile : dps) {
            Node m = allNodesMap.get(dpfile); if(m==null) allNodesMap.put(dpfile, m=new Node(dpfile));
            n.addEdge(m);
        }
        if(n.outEdges.size()==0) S.add(n);
    }
    
    // Get AllNodes Set
    Set <Node> allNodes = new HashSet(allNodesMap.values());
    allNodes.removeAll(S);
    
    // If S set empty, add small cycle
    if(S.isEmpty() && !allNodes.isEmpty()) {
        Set <Node> smallCycle = getSmallCycle(allNodes);
        S.addAll(smallCycle); allNodes.removeAll(smallCycle);
    }

    // L <- Empty list that will contain the sorted elements
    List<Node> L = new ArrayList<Node>();
    
    // While S is non-empty do
    while(!S.isEmpty()) {
        
        // Remove a node n from S and add to L
        Node n = S.iterator().next();
        S.remove(n);
        L.add(n); //System.out.println(L.size() + ". " + n.item);

        // Clear node OutEdges
        if(n.outEdges.size()>0) for(Iterator<Node> it = n.outEdges.iterator(); it.hasNext();) { Node m = it.next();
            it.remove(); m.inEdges.remove(n); }
        
        // Clear node InEdges
        for(Iterator<Node> it = n.inEdges.iterator(); it.hasNext();) { Node m = it.next();
            it.remove(); m.outEdges.remove(n);
            if(m.outEdges.isEmpty()) {  // If dependent has no other dependencies then add to S
                S.add(m); allNodes.remove(m); }
        }
        
        // If S set empty, add small cycle
        if(S.isEmpty() && !allNodes.isEmpty()) {
            Set <Node> smallestCycle = getSmallCycle(allNodes);
            S.addAll(smallestCycle); allNodes.removeAll(smallestCycle);
        }
    }
    
    // Reset Items with new ordering
    theItems.clear();
    for(Node n : L) theItems.add((WebFile)n.item);
    if(allNodes.size()>0) throw new RuntimeException("TopoSort: Items left!");
}

/**
 * Returns a small cycle in given node set.
 */
private static Set <Node> getSmallCycle(Set <Node> theNodes)
{
    Set <Node> cycle = new HashSet(), small = null;
    for(Node n : theNodes) {
        getCycle(n, cycle);
        if(small==null || cycle.size()<small.size()) { small = cycle; cycle = new HashSet(); } else cycle.clear();
        if(small.size()<50) break;  // We don't need the smallest cycle
    }
        
    //System.out.println("Cycle:"); for(Node p : small) System.out.println("    " + p);
    //System.out.println("Cycle: " + small.size() + " from " + theNodes.size());
    return small;
}

/**
 * Returns the smallest cycle in given node set.
 */
private static void getCycle(Node aNode, Set <Node> theNodes)
{
    theNodes.add(aNode);
    for(Node e : (Set<Node>)aNode.outEdges)
        if(!theNodes.contains(e))
            getCycle(e, theNodes);
}

/**
 * A class for node.
 */
private static class Node <T> {
    public final T item;
    public final Set<Node> inEdges = new HashSet<Node>(), outEdges = new HashSet<Node>();
    public Node(T anItem)  { this.item = anItem; }
    public Node addEdge(Node node)  { outEdges.add(node); node.inEdges.add(this); return this; }
    public int hashCode()  { return item.hashCode(); }
    public boolean equals(Object obj)  { Node other = (Node)obj; return item.equals(other.item); }
    public String toString() { return this.item.toString(); }
}

}