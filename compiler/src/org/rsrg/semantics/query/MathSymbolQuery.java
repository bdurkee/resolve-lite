package org.rsrg.semantics.query;

import org.antlr.v4.runtime.Token;
import org.rsrg.semantics.MathSymbolTableBuilder;
import org.rsrg.semantics.PossiblyQualifiedPath;
import org.rsrg.semantics.MathSymbolTableBuilder.ImportStrategy;
import org.rsrg.semantics.searchers.NameSearcher;
import org.rsrg.semantics.symbol.MathSymbol;
import org.rsrg.semantics.symbol.Symbol;

public class MathSymbolQuery extends ResultProcessingQuery<Symbol, MathSymbol> {

    public MathSymbolQuery(Token qualifier, Token name) {
        this(qualifier, name.getText(), name);
    }

    public MathSymbolQuery(Token qualifier, String name, Token l) {
        super(new BaseSymbolQuery<Symbol>(new PossiblyQualifiedPath(qualifier,
                ImportStrategy.IMPORT_NAMED, MathSymbolTableBuilder.FacilityStrategy.FACILITY_IGNORE,
                true), new NameSearcher(name, true)), Symbol::toMathSymbol);
    }
}