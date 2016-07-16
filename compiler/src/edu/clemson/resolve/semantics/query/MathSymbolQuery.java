package edu.clemson.resolve.semantics.query;

import org.antlr.v4.runtime.Token;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import edu.clemson.resolve.semantics.PossiblyQualifiedPath;
import edu.clemson.resolve.semantics.searchers.NameSearcher;
import edu.clemson.resolve.semantics.symbol.MathClssftnWrappingSymbol;
import edu.clemson.resolve.semantics.symbol.Symbol;

import static edu.clemson.resolve.semantics.MathSymbolTable.FacilityStrategy.FACILITY_IGNORE;
import static edu.clemson.resolve.semantics.MathSymbolTable.ImportStrategy.IMPORT_NAMED;

public class MathSymbolQuery extends ResultProcessingQuery<Symbol, MathClssftnWrappingSymbol> {

    public MathSymbolQuery(@Nullable Token qualifier, @NotNull Token name) {
        this(qualifier, name.getText());
    }

    public MathSymbolQuery(@Nullable Token qualifier, @NotNull String name) {
        super(new BaseSymbolQuery<Symbol>(new PossiblyQualifiedPath(qualifier,
                IMPORT_NAMED, FACILITY_IGNORE, true), new NameSearcher(name, true)), Symbol::toMathSymbol);
    }
}