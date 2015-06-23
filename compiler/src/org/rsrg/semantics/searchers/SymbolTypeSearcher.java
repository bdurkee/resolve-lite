package org.rsrg.semantics.searchers;

import org.rsrg.semantics.symbol.FacilitySymbol;
import org.rsrg.semantics.symbol.Symbol;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SymbolTypeSearcher<E extends Symbol>
        implements
            MultimatchTableSearcher<E> {

    public static final SymbolTypeSearcher<FacilitySymbol> FACILITY_SEARCHER =
            new SymbolTypeSearcher<>(FacilitySymbol.class);

    private final Class<E> targetClass;

    public SymbolTypeSearcher(Class<E> targetClass) {
        this.targetClass = targetClass;
    }

    @Override public boolean addMatches(Map<String, Symbol> entries,
                            List<E> matches, SearchContext l) {
        matches.addAll(entries.values().stream().filter(targetClass::isInstance)
                .map(targetClass::cast)
                .collect(Collectors.toList()));
        return false;
    }
}