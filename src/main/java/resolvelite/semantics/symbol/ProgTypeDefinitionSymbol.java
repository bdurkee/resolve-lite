package resolvelite.semantics.symbol;

import resolvelite.semantics.SymbolTable;
import resolvelite.semantics.Type;
import resolvelite.semantics.symbol.SymbolWithScope;

public class ProgTypeDefinitionSymbol extends SymbolWithScope implements Type {

    public ProgTypeDefinitionSymbol(String name, SymbolTable scopeRepo) {
        super(name, scopeRepo, "GLOBAL");//this will change the root id that is.
    }

}
