package org.resolvelite.semantics.query;

import org.antlr.v4.runtime.Token;
import org.resolvelite.semantics.PossiblyQualifiedPath;
import org.resolvelite.semantics.SymbolTable;
import org.resolvelite.semantics.SymbolTable.ImportStrategy;
import org.resolvelite.semantics.searchers.NameSearcher;
import org.resolvelite.semantics.symbol.MathSymbol;
import org.resolvelite.semantics.symbol.Symbol;

import java.util.function.Function;

public class MathSymbolQuery extends ResultProcessingQuery<Symbol, MathSymbol> {

    public MathSymbolQuery(Token qualifier, Token name) {
        this(qualifier, name.getText(), name);
    }

    public MathSymbolQuery(Token qualifier, String name, Token l) {
        super(new BaseSymbolQuery<Symbol>(new PossiblyQualifiedPath(qualifier,
                ImportStrategy.IMPORT_NAMED, SymbolTable.FacilityStrategy.FACILITY_IGNORE,
                true), new NameSearcher(name, true)), Symbol::toMathSymbol);
    }
}