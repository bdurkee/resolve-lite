package org.rsrg.semantics.query;

import org.rsrg.semantics.MathSymbolTable.FacilityStrategy;
import org.rsrg.semantics.MathSymbolTable.ImportStrategy;
import org.rsrg.semantics.UnqualifiedPath;
import org.rsrg.semantics.searchers.NameSearcher;
import org.rsrg.semantics.symbol.Symbol;

public class UnqualifiedNameQuery extends BaseMultimatchSymbolQuery<Symbol> {

    public UnqualifiedNameQuery(String searchString,
                                ImportStrategy importStrategy, FacilityStrategy facilityStrategy,
                                boolean stopAfterFirst, boolean localPriority) {
        super(new UnqualifiedPath(importStrategy, facilityStrategy,
                localPriority), new NameSearcher(searchString, stopAfterFirst));
    }

    public UnqualifiedNameQuery(String searchString) {
        this(searchString, ImportStrategy.IMPORT_NAMED,
                FacilityStrategy.FACILITY_IGNORE, true, true);
    }
}
