package edu.clemson.resolve.semantics.searchers;

import org.jetbrains.annotations.NotNull;
import edu.clemson.resolve.semantics.symbol.FacilitySymbol;
import edu.clemson.resolve.semantics.symbol.Symbol;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SymbolTypeSearcher<E extends Symbol>
        implements
            MultimatchTableSearcher<E> {

    public static final SymbolTypeSearcher<FacilitySymbol> FACILITY_SEARCHER =
            new SymbolTypeSearcher<>(FacilitySymbol.class);

    @NotNull private final Class<E> targetClass;

    public SymbolTypeSearcher(@NotNull Class<E> targetClass) {
        this.targetClass = targetClass;
    }

    @Override public boolean addMatches(@NotNull Map<String, Symbol> entries,
                                        @NotNull List<E> matches,
                                        @NotNull SearchContext l) {
        matches.addAll(entries.values().stream().filter(targetClass::isInstance)
                .map(targetClass::cast)
                .collect(Collectors.toList()));
        return false;
    }
}