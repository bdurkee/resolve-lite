package org.resolvelite.semantics.searchers;

import org.resolvelite.semantics.SymbolTable;
import org.resolvelite.semantics.symbol.GenericSymbol;
import org.resolvelite.semantics.symbol.ProgTypeSymbol;
import org.resolvelite.semantics.symbol.Symbol;

import java.util.Iterator;
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