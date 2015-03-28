package resolvelite.semantics.symbol;

import resolvelite.semantics.SymbolTable;
import resolvelite.semantics.Type;
import resolvelite.semantics.symbol.SymbolWithScope;

public class ProgTypeSymbol extends SymbolWithScope implements Type {

    public ProgTypeSymbol(String name, SymbolTable scopeRepo,
            String rootModuleID) {
        super(name, scopeRepo, rootModuleID);
    }

}
