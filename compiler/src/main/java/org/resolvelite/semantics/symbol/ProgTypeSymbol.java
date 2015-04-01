package org.resolvelite.semantics.symbol;

import org.resolvelite.semantics.SymbolTable;
import org.resolvelite.semantics.Type;

public class ProgTypeSymbol extends SymbolWithScope implements Type {

    public ProgTypeSymbol(String name, SymbolTable scopeRepo,
            String rootModuleID) {
        super(name, scopeRepo, rootModuleID);
    }

}
