package org.resolvelite.semantics;

/**
 * A scope to hold predefined symbols for RESOLVE. In practice, this just ends
 * up being a bunch of math operators and types basic or fundamental enough to
 * hardcode in.
 */
public class PredefinedScope extends BaseScope {

    public PredefinedScope(SymbolTable scopeRepo) {
        super(scopeRepo, "GLOBAL");
    }
    
    @Override public String getScopeDescription() {
        return "predefined";
    }
}
