package edu.clemson.resolve.semantics.query;

import org.antlr.v4.runtime.Token;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import edu.clemson.resolve.semantics.PossiblyQualifiedPath;
import edu.clemson.resolve.semantics.MathSymbolTable.FacilityStrategy;
import edu.clemson.resolve.semantics.MathSymbolTable.ImportStrategy;
import edu.clemson.resolve.semantics.searchers.NameSearcher;
import edu.clemson.resolve.semantics.symbol.Symbol;

/**
 * A {@code NameQuery} takes a (possibly-null) qualifier and a name and searches for entries that match. If the
 * qualifier is non-null, the appropriate facility or module is searched. If it <em>is</em> null, a search is performed
 * using the provided {@code ImportStrategy} and {@code FacilityStrategy}.
 */
public class NameQuery extends BaseMultimatchSymbolQuery<Symbol> implements MultimatchSymbolQuery<Symbol> {

    public NameQuery(@Nullable Token qualifier,
                     @NotNull String name,
                     @NotNull ImportStrategy importStrategy,
                     @NotNull FacilityStrategy facilityStrategy,
                     boolean localPriority) {
        super(new PossiblyQualifiedPath(qualifier, importStrategy,
                facilityStrategy, localPriority), new NameSearcher(name, false));
    }

    public NameQuery(@Nullable Token qualifier, @NotNull String name, boolean localPriority) {
        this(qualifier, name, ImportStrategy.IMPORT_NAMED, FacilityStrategy.FACILITY_IGNORE, localPriority);
    }
}