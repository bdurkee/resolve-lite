package org.rsrg.semantics.query;

import org.antlr.v4.runtime.Token;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rsrg.semantics.MathSymbolTable;
import org.rsrg.semantics.PossiblyQualifiedPath;
import org.rsrg.semantics.UnexpectedSymbolException;
import org.rsrg.semantics.searchers.NameSearcher;
import org.rsrg.semantics.symbol.ProgVariableSymbol;
import org.rsrg.semantics.symbol.Symbol;

/**
 * Created by daniel
 */
public class ProgVariableQuery
        extends
            ResultProcessingQuery<Symbol, ProgVariableSymbol> {

    public ProgVariableQuery(@Nullable Token qualifier, @NotNull Token name,
                             boolean b) throws UnexpectedSymbolException {
        this(qualifier, name.getText());
    }

    public ProgVariableQuery(@Nullable Token qualifier, @NotNull String name) {
        super(new BaseSymbolQuery<Symbol>(new PossiblyQualifiedPath(
                        qualifier, MathSymbolTable.ImportStrategy.IMPORT_NAMED,
                        MathSymbolTable.FacilityStrategy.FACILITY_IGNORE, true),
                        new NameSearcher(name, true)),
                Symbol::toProgVariableSymbol);
    }
}
