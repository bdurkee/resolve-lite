package org.rsrg.semantics.query;

import org.rsrg.semantics.SymbolTable;
import org.rsrg.semantics.UnqualifiedPath;
import org.rsrg.semantics.searchers.GenericSearcher;
import org.rsrg.semantics.symbol.ProgTypeSymbol;

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