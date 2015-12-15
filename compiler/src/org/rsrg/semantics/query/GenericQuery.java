package org.rsrg.semantics.query;

import org.jetbrains.annotations.NotNull;
import org.rsrg.semantics.MathSymbolTable;
import org.rsrg.semantics.UnqualifiedPath;
import org.rsrg.semantics.searchers.GenericSearcher;
import org.rsrg.semantics.symbol.ProgTypeSymbol;

public class GenericQuery extends BaseMultimatchSymbolQuery<ProgTypeSymbol>
        implements
            MultimatchSymbolQuery<ProgTypeSymbol> {

    @NotNull public static final GenericQuery INSTANCE = new GenericQuery();

    private GenericQuery() {
        super(new UnqualifiedPath(MathSymbolTable.ImportStrategy.IMPORT_NAMED,
                MathSymbolTable.FacilityStrategy.FACILITY_IGNORE, true),
                GenericSearcher.INSTANCE);
    }
}