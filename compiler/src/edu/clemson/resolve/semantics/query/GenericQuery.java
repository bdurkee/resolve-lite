package edu.clemson.resolve.semantics.query;

import org.jetbrains.annotations.NotNull;
import edu.clemson.resolve.semantics.MathSymbolTable;
import edu.clemson.resolve.semantics.UnqualifiedPath;
import edu.clemson.resolve.semantics.searchers.GenericSearcher;
import edu.clemson.resolve.semantics.symbol.ProgTypeSymbol;

public class GenericQuery extends BaseMultimatchSymbolQuery<ProgTypeSymbol>
        implements
        MultimatchSymbolQuery<ProgTypeSymbol> {

    @NotNull
    public static final GenericQuery INSTANCE = new GenericQuery();

    private GenericQuery() {
        super(new UnqualifiedPath(MathSymbolTable.ImportStrategy.IMPORT_NAMED,
                        MathSymbolTable.FacilityStrategy.FACILITY_IGNORE, true),
                GenericSearcher.INSTANCE);
    }
}