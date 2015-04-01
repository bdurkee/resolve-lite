package org.resolvelite.semantics;

public class LocalScope extends BaseScope {

    public LocalScope(Scope enclosingScope, SymbolTable scopeRepo,
            String rootModuleID) {
        super(enclosingScope, scopeRepo, rootModuleID);
    }

    @Override public String getScopeDescription() {
        return "local";
    }
}
