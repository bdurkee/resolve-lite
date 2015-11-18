package org.rsrg.semantics.query;

import org.rsrg.semantics.MathSymbolTableBuilder;
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
            Class<? extends Symbol> entryType, MathSymbolTableBuilder.ImportStrategy importStrategy,
            MathSymbolTableBuilder.FacilityStrategy facilityStrategy) {
        super(new UnqualifiedPath(importStrategy, facilityStrategy, false),
                new SymbolTypeSearcher(entryType));
    }

    public SymbolTypeQuery(Class<? extends Symbol> entryType) {
        this(entryType, MathSymbolTableBuilder.ImportStrategy.IMPORT_NAMED,
                MathSymbolTableBuilder.FacilityStrategy.FACILITY_INSTANTIATE);
    }
}