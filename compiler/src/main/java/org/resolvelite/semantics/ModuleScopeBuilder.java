package org.resolvelite.semantics;

import org.antlr.v4.runtime.tree.ParseTree;
import org.resolvelite.compiler.tree.AnnotatedTree;
import org.resolvelite.typereasoning.TypeGraph;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class ModuleScopeBuilder extends ScopeBuilder {

    private SymbolTable symtab;
    private final List<String> importedModules = new LinkedList<>();
    private final AnnotatedTree annotatedTree;

    ModuleScopeBuilder(TypeGraph g, String name, ParseTree definingTree,
                       AnnotatedTree annotatedTree,
            Scope parent, SymbolTable symbolTable) {
        super(symbolTable, g, definingTree, parent, name);
        this.symtab = symbolTable;
        this.annotatedTree = annotatedTree;
    }

    public AnnotatedTree getWrappedModuleTree() {
        return annotatedTree;
    }

    public String getModuleID() {
        return moduleID;
    }

    public ModuleScopeBuilder addImports(List<String> importList) {
        importList.stream().filter(u ->
                !importedModules.contains(u) && !moduleID.equals(u))
                .forEach(importedModules::add);
        return this;
    }

    public ModuleScopeBuilder addImports(String... i) {
        return addImports(Arrays.asList(i));
    }

    public boolean imports(String i) {
        return i.equals(getModuleID()) || importedModules.contains(i);
    }

    public List<String> getImports() {
        return new LinkedList<String>(importedModules);
    }

    @Override public String toString() {
        return moduleID + ":" + symbols.keySet();
    }

}
