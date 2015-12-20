package org.rsrg.semantics.searchers;

import org.jetbrains.annotations.NotNull;
import org.rsrg.semantics.UnexpectedSymbolException;
import org.rsrg.semantics.symbol.ProgParameterSymbol;
import org.rsrg.semantics.symbol.ProgTypeSymbol;
import org.rsrg.semantics.symbol.Symbol;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GenericSearcher
        implements
            MultimatchTableSearcher<ProgTypeSymbol> {

    public static final GenericSearcher INSTANCE = new GenericSearcher();
    private GenericSearcher() {}

    @Override public boolean addMatches(@NotNull Map<String, Symbol> entries,
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