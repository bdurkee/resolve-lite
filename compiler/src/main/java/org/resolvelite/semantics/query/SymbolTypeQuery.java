package org.resolvelite.semantics.query;

import org.resolvelite.semantics.SymbolTable;
import org.resolvelite.semantics.UnqualifiedPath;
import org.resolvelite.semantics.searchers.SymbolTypeSearcher;
import org.resolvelite.semantics.symbol.Symbol;
import org.resolvelite.semantics.SymbolTable.ImportStrategy;
import org.resolvelite.semantics.SymbolTable.FacilityStrategy;

/**
 * 
 * @author hamptos
 */
public class SymbolTypeQuery<T extends Symbol>
        extends
            BaseMultimatchSymbolQuery<T> implements MultimatchSymbolQuery<T> {

    @SuppressWarnings("unchecked") public SymbolTypeQuery(
            Class<? extends Symbol> entryType, ImportStrategy importStrategy,
            FacilityStrategy facilityStrategy) {
        super(new UnqualifiedPath(importStrategy, facilityStrategy, false),
                new SymbolTypeSearcher(entryType));
    }

    public SymbolTypeQuery(Class<? extends Symbol> entryType) {
        this(entryType, ImportStrategy.IMPORT_NAMED,
                FacilityStrategy.FACILITY_GENERIC);
    }
}
