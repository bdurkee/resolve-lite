package org.rsrg.semantics;

import org.antlr.v4.runtime.ParserRuleContext;

import java.util.*;

public class ModuleScopeBuilder extends ScopeBuilder {

    private final List<String> importedModules = new LinkedList<>();

    ModuleScopeBuilder(TypeGraph g, String name, ParserRuleContext definingTree,
                       Scope parent, SymbolTable symbolTable) {
        super(symbolTable, g, definingTree, parent, name);
    }

    public String getModuleID() {
        return moduleID;
    }

    public ModuleScopeBuilder addImports(Collection<String> imports) {
        importedModules.addAll(imports);
        return this;
    }

    public boolean imports(String i) {
        return i.equals(getModuleID()) || importedModules.contains(i);
    }

    public List<String> getImports() {
        return new ArrayList<>(importedModules);
    }

    @Override public String toString() {
        return moduleID+":"+symbols.keySet();
    }

}
