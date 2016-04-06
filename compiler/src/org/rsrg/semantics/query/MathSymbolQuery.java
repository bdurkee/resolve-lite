package org.rsrg.semantics.query;

import org.antlr.v4.runtime.Token;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rsrg.semantics.MathSymbolTable;
import org.rsrg.semantics.PossiblyQualifiedPath;
import org.rsrg.semantics.MathSymbolTable.ImportStrategy;
import org.rsrg.semantics.searchers.NameSearcher;
import org.rsrg.semantics.symbol.MathSymbol;
import org.rsrg.semantics.symbol.Symbol;

import static org.rsrg.semantics.MathSymbolTable.FacilityStrategy.FACILITY_IGNORE;
import static org.rsrg.semantics.MathSymbolTable.ImportStrategy.IMPORT_NAMED;

public class MathSymbolQuery extends ResultProcessingQuery<Symbol, MathSymbol> {

    public MathSymbolQuery(@Nullable Token qualifier, @NotNull Token name) {
        this(qualifier, name.getText(), name);
    }

    public MathSymbolQuery(@Nullable Token qualifier, @NotNull String name,
                           @NotNull Token l) {
        super(new BaseSymbolQuery<Symbol>(new PossiblyQualifiedPath(qualifier,
                IMPORT_NAMED, FACILITY_IGNORE, true),
                    new NameSearcher(name, false)), Symbol::toMathSymbol);
    }
}