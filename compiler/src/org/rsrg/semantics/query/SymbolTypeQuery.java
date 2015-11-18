package org.rsrg.semantics.query;

import org.rsrg.semantics.MathSymbolTable;
import org.rsrg.semantics.UnqualifiedPath;
import org.rsrg.semantics.searchers.SymbolTypeSearcher;
import org.rsrg.semantics.symbol.Symbol;

/**
 *
 * @author hamptos
 */
public class SymbolTypeQuery<T extends Symbol>
        extends
            BaseMultimatchSymbolQuery<T> implements MultimatchSymbolQuery<T> {

    @SuppressWarnings("unchecked") public SymbolTypeQuery(
            Class<? extends Symbol> entryType, MathSymbolTable.ImportStrategy importStrategy,
            MathSymbolTable.FacilityStrategy facilityStrategy) {
        super(new UnqualifiedPath(importStrategy, facilityStrategy, false),
                new SymbolTypeSearcher(entryType));
    }

    public SymbolTypeQuery(Class<? extends Symbol> entryType) {
        this(entryType, MathSymbolTable.ImportStrategy.IMPORT_NAMED,
                MathSymbolTable.FacilityStrategy.FACILITY_INSTANTIATE);
    }
}