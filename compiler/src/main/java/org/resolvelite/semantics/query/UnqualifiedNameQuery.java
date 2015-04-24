package org.resolvelite.semantics.query;

import org.resolvelite.semantics.ScopeSearchPath;
import org.resolvelite.semantics.SymbolTable;
import org.resolvelite.semantics.SymbolTable.ImportStrategy;
import org.resolvelite.semantics.SymbolTable.FacilityStrategy;
import org.resolvelite.semantics.UnqualifiedPath;
import org.resolvelite.semantics.searchers.NameSearcher;
import org.resolvelite.semantics.searchers.TableSearcher;
import org.resolvelite.semantics.symbol.Symbol;

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
