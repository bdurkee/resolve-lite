package org.rsrg.semantics.query;

import org.rsrg.semantics.SymbolTable;
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
            Class<? extends Symbol> entryType, SymbolTable.ImportStrategy importStrategy,
            SymbolTable.FacilityStrategy facilityStrategy) {
        super(new UnqualifiedPath(importStrategy, facilityStrategy, false),
                new SymbolTypeSearcher(entryType));
    }

    public SymbolTypeQuery(Class<? extends Symbol> entryType) {
        this(entryType, SymbolTable.ImportStrategy.IMPORT_NAMED,
                SymbolTable.FacilityStrategy.FACILITY_GENERIC);
    }
}