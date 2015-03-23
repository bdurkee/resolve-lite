package resolvelite.semantics.symbol;

import resolvelite.semantics.Type;
import resolvelite.semantics.symbol.SymbolWithScope;

public class ProgTypeDefinitionSymbol extends SymbolWithScope implements Type {

    public ProgTypeDefinitionSymbol(String name) {
        super(name);
    }
}
