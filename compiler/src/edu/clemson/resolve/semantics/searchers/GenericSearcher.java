package edu.clemson.resolve.semantics.searchers;

import org.jetbrains.annotations.NotNull;
import edu.clemson.resolve.semantics.UnexpectedSymbolException;
import edu.clemson.resolve.semantics.symbol.ProgParameterSymbol;
import edu.clemson.resolve.semantics.symbol.ProgTypeSymbol;
import edu.clemson.resolve.semantics.symbol.Symbol;

import java.util.List;
import java.util.Map;

public class GenericSearcher
        implements
        MultimatchTableSearcher<ProgTypeSymbol> {

    public static final GenericSearcher INSTANCE = new GenericSearcher();

    private GenericSearcher() {
    }

    @Override
    public boolean addMatches(@NotNull Map<String, Symbol> entries,
                              @NotNull List<ProgTypeSymbol> matches,
                              @NotNull SearchContext l)
            throws UnexpectedSymbolException {
        for (Symbol s : entries.values()) {
            if (s instanceof ProgParameterSymbol) {
                if (((ProgParameterSymbol) s).getMode() ==
                        ProgParameterSymbol.ParameterMode.TYPE) {
                    matches.add(s.toProgTypeSymbol());
                }
            }
        }
        return false;
    }
}