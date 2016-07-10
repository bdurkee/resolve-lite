package edu.clemson.resolve.semantics.query;

import org.jetbrains.annotations.NotNull;
import edu.clemson.resolve.semantics.MathSymbolTable.FacilityStrategy;
import edu.clemson.resolve.semantics.MathSymbolTable.ImportStrategy;
import edu.clemson.resolve.semantics.UnqualifiedPath;
import edu.clemson.resolve.semantics.searchers.SymbolTypeSearcher;
import edu.clemson.resolve.semantics.symbol.Symbol;

import static edu.clemson.resolve.semantics.MathSymbolTable.FacilityStrategy.FACILITY_INSTANTIATE;
import static edu.clemson.resolve.semantics.MathSymbolTable.ImportStrategy.IMPORT_NAMED;

/**
 * @author hamptos
 */
public class SymbolTypeQuery<T extends Symbol> extends BaseMultimatchSymbolQuery<T> implements MultimatchSymbolQuery<T> {

    @SuppressWarnings("unchecked")
    public SymbolTypeQuery(@NotNull Class<? extends Symbol> entryType,
                           @NotNull ImportStrategy importStrategy,
                           @NotNull FacilityStrategy facilityStrategy) {
        super(new UnqualifiedPath(importStrategy, facilityStrategy, false), new SymbolTypeSearcher(entryType));
    }

    public SymbolTypeQuery(Class<? extends Symbol> entryType) {
        this(entryType, IMPORT_NAMED, FACILITY_INSTANTIATE);
    }
}