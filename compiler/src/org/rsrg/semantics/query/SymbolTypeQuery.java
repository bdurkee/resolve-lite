package org.rsrg.semantics.query;

import org.jetbrains.annotations.NotNull;
import org.rsrg.semantics.MathSymbolTable;
import org.rsrg.semantics.MathSymbolTable.FacilityStrategy;
import org.rsrg.semantics.MathSymbolTable.ImportStrategy;
import org.rsrg.semantics.UnqualifiedPath;
import org.rsrg.semantics.searchers.SymbolTypeSearcher;
import org.rsrg.semantics.symbol.Symbol;

import static org.rsrg.semantics.MathSymbolTable.FacilityStrategy.FACILITY_INSTANTIATE;
import static org.rsrg.semantics.MathSymbolTable.ImportStrategy.IMPORT_NAMED;

/**
 *
 * @author hamptos
 */
public class SymbolTypeQuery<T extends Symbol>
        extends
            BaseMultimatchSymbolQuery<T> implements MultimatchSymbolQuery<T> {

    @SuppressWarnings("unchecked") public SymbolTypeQuery(
            @NotNull Class<? extends Symbol> entryType,
            @NotNull ImportStrategy importStrategy,
            @NotNull FacilityStrategy facilityStrategy) {
        super(new UnqualifiedPath(importStrategy, facilityStrategy, false),
                new SymbolTypeSearcher(entryType));
    }

    public SymbolTypeQuery(Class<? extends Symbol> entryType) {
        this(entryType, IMPORT_NAMED, FACILITY_INSTANTIATE);
    }
}