package org.resolvelite.semantics;

import org.antlr.v4.runtime.tree.ParseTree;
import org.resolvelite.semantics.symbol.Symbol;
import org.resolvelite.typereasoning.TypeGraph;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class BaseScope implements Scope {

    protected final Map<String, Symbol> symbols;
    private final SymbolTable symtab;

    protected ParseTree definingTree;
    protected Scope parent;
    protected final String moduleID;

    BaseScope(SymbolTable scopeRepo, ParseTree definingTree, Scope parent,
            String moduleID, Map<String, Symbol> bindingSyms) {
        this.symtab = scopeRepo;
        this.symbols = bindingSyms;
        this.parent = parent;
        this.moduleID = moduleID;
        this.definingTree = definingTree;
    }

    public ParseTree getDefiningTree() {
        return definingTree;
    }

    public String getModuleID() {
        return moduleID;
    }

    @Override public Symbol define(Symbol s) throws DuplicateSymbolException {
        if ( symbols.containsKey(s.getName()) ) {
            throw new DuplicateSymbolException();
        }
        symbols.put(s.getName(), s);
        return s;
    }

    @Override public <T extends Symbol> List<T> getSymbolsOfType(Class<T> type) {
        return symbols.values().stream()
                .filter(type::isInstance)
                .map(type::cast)
                .collect(Collectors.toList());
    }

    @Override public String toString() {
        return symbols.keySet() + "";
    }
}
