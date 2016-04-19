package edu.clemson.resolve.semantics.query;

import org.jetbrains.annotations.NotNull;
import edu.clemson.resolve.semantics.MathSymbolTable.FacilityStrategy;
import edu.clemson.resolve.semantics.MathSymbolTable.ImportStrategy;
import edu.clemson.resolve.semantics.UnqualifiedPath;
import edu.clemson.resolve.semantics.searchers.NameSearcher;
import edu.clemson.resolve.semantics.symbol.Symbol;

public class UnqualifiedNameQuery extends BaseMultimatchSymbolQuery<Symbol> {

    public UnqualifiedNameQuery(@NotNull String searchString,
                                @NotNull ImportStrategy importStrategy,
                                @NotNull FacilityStrategy facilityStrategy,
                                boolean stopAfterFirst, boolean localPriority) {
        super(new UnqualifiedPath(importStrategy, facilityStrategy,
                localPriority), new NameSearcher(searchString, stopAfterFirst));
    }

    public UnqualifiedNameQuery(@NotNull String searchString) {
        this(searchString, ImportStrategy.IMPORT_NAMED,
                FacilityStrategy.FACILITY_IGNORE, true, true);
    }
}
