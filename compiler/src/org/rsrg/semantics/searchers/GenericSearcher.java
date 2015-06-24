package org.rsrg.semantics.searchers;

import org.rsrg.semantics.symbol.GenericSymbol;
import org.rsrg.semantics.symbol.ProgTypeSymbol;
import org.rsrg.semantics.symbol.Symbol;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GenericSearcher implements MultimatchTableSearcher<ProgTypeSymbol> {

    public static final GenericSearcher INSTANCE = new GenericSearcher();

    private GenericSearcher() {}

    @Override public boolean addMatches(Map<String, Symbol> entries,
                              List<ProgTypeSymbol> matches, SearchContext l) {
        matches.addAll(entries.values().stream()
                .filter(s -> s instanceof GenericSymbol)
                .map(Symbol::toProgTypeSymbol)
                .collect(Collectors.toList()));
        return false;
    }
}