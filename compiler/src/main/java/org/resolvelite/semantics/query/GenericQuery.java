package org.resolvelite.semantics.query;

import org.resolvelite.semantics.SymbolTable;
import org.resolvelite.semantics.UnqualifiedPath;
import org.resolvelite.semantics.searchers.GenericSearcher;
import org.resolvelite.semantics.symbol.ProgTypeSymbol;

public class GenericQuery extends BaseMultimatchSymbolQuery<ProgTypeSymbol>
        implements
            MultimatchSymbolQuery<ProgTypeSymbol> {

    public static final GenericQuery INSTANCE = new GenericQuery();

    private GenericQuery() {
        super(new UnqualifiedPath(SymbolTable.ImportStrategy.IMPORT_NAMED,
                SymbolTable.FacilityStrategy.FACILITY_IGNORE, true),
                GenericSearcher.INSTANCE);
    }
}