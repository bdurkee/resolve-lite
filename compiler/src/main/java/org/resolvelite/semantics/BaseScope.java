package org.resolvelite.semantics;

import org.antlr.v4.runtime.tree.ParseTree;
import org.resolvelite.semantics.symbol.Symbol;
import org.resolvelite.typereasoning.TypeGraph;

import java.util.HashMap;
import java.util.Map;

public abstract class BaseScope implements Scope {

    private final Map<String, Symbol> symbols;
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
    }

    public ParseTree getDefiningTree() {
        return definingTree;
    }

}
