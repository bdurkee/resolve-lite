package org.rsrg.semantics.query;

import org.antlr.v4.runtime.Token;
import org.rsrg.semantics.MathSymbolTable;
import org.rsrg.semantics.PossiblyQualifiedPath;
import org.rsrg.semantics.searchers.NameSearcher;
import org.rsrg.semantics.symbol.ProgVariableSymbol;
import org.rsrg.semantics.symbol.Symbol;

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
                        qualifier, MathSymbolTable.ImportStrategy.IMPORT_NAMED,
                        MathSymbolTable.FacilityStrategy.FACILITY_IGNORE, true),
                        new NameSearcher(name, true)),
                Symbol::toProgVariableSymbol);
    }
}
