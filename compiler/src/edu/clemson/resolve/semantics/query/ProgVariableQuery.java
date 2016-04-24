package edu.clemson.resolve.semantics.query;

import org.antlr.v4.runtime.Token;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import edu.clemson.resolve.semantics.MathSymbolTable;
import edu.clemson.resolve.semantics.PossiblyQualifiedPath;
import edu.clemson.resolve.semantics.UnexpectedSymbolException;
import edu.clemson.resolve.semantics.searchers.NameSearcher;
import edu.clemson.resolve.semantics.symbol.ProgVariableSymbol;
import edu.clemson.resolve.semantics.symbol.Symbol;

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
