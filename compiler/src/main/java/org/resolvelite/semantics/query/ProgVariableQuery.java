package org.resolvelite.semantics.query;

import org.antlr.v4.runtime.Token;
import org.resolvelite.semantics.PossiblyQualifiedPath;
import org.resolvelite.semantics.SymbolTable;
import org.resolvelite.semantics.searchers.NameSearcher;
import org.resolvelite.semantics.symbol.ProgVariableSymbol;
import org.resolvelite.semantics.symbol.Symbol;

/**
 * Created by daniel
 */
public class ProgVariableQuery
        extends
            ResultProcessingQuery<Symbol, ProgVariableSymbol> {

    public ProgVariableQuery(Token qualifier, Token name, boolean b) {
        this(qualifier, name.getText());
    }

    public ProgVariableQuery(Token qualifier, String name) {
        super(new BaseSymbolQuery<Symbol>(new PossiblyQualifiedPath(
                        qualifier, SymbolTable.ImportStrategy.IMPORT_NAMED,
                        SymbolTable.FacilityStrategy.FACILITY_IGNORE, true),
                        new NameSearcher(name, true)),
                Symbol::toProgVariableSymbol);
    }
}
