package org.resolvelite.semantics;

import org.resolvelite.compiler.tree.AnnotatedTree;

import java.util.*;

public class ModuleScope extends BaseScope {
    private final Set<String> importedModules = new LinkedHashSet<>();
    private final AnnotatedTree wrappedTree;

    public ModuleScope(Scope scope, SymbolTable scopeRepo, AnnotatedTree m) {
        super(scope, scopeRepo, m.getName());
        this.wrappedTree = m;
    }

    @Override public String getScopeDescription() {
        return "module";
    }

    public AnnotatedTree getWrappedModuleTree() {
        return wrappedTree;
    }

    public ModuleScope addImports(String... s) {
        return addImports(new LinkedHashSet<String>(Arrays.asList(s)));
    }

    public ModuleScope addImports(Set<String> imports) {
        imports.forEach(importedModules::add);
        return this;
    }

    public Set<String> getImports() {
        return importedModules;
    }
}
