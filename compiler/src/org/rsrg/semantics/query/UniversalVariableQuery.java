package org.rsrg.semantics.query;

import org.jetbrains.annotations.NotNull;
import org.rsrg.semantics.*;
import org.rsrg.semantics.searchers.MultimatchTableSearcher;
import org.rsrg.semantics.symbol.MathSymbol;
import org.rsrg.semantics.symbol.Symbol;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UniversalVariableQuery
        implements
            MultimatchSymbolQuery<MathSymbol> {

    public static final MultimatchSymbolQuery<MathSymbol> INSTANCE =
            (MultimatchSymbolQuery<MathSymbol>) new UniversalVariableQuery();

    private final BaseSymbolQuery<MathSymbol> myBaseQuery;

    private UniversalVariableQuery() {
        myBaseQuery =
                new BaseSymbolQuery<MathSymbol>(new UnqualifiedPath(
                        MathSymbolTable.ImportStrategy.IMPORT_NONE,
                        MathSymbolTable.FacilityStrategy.FACILITY_IGNORE, false),
                        new UniversalVariableSearcher());
    }

    @Override public List<MathSymbol> searchFromContext(@NotNull Scope source,
            @NotNull MathSymbolTable repo) {

        List<MathSymbol> result;
        try {
            result = myBaseQuery.searchFromContext(source, repo);
        }
        catch (DuplicateSymbolException dse) {
            //Can't happen--our base query is a name matcher
            throw new RuntimeException(dse);
        }

        return result;
    }

    private static class UniversalVariableSearcher
            implements
            MultimatchTableSearcher<MathSymbol> {

        @Override public boolean addMatches(@NotNull Map<String, Symbol> entries,
                                  @NotNull List<MathSymbol> matches, @NotNull SearchContext l) {

            List<MathSymbol> mathSymbols = entries.values().stream()
                    .filter(s -> s instanceof MathSymbol)
                    .map(Symbol::toMathSymbol).collect(Collectors.toList());

            matches.addAll(mathSymbols.stream()
                    .filter(s -> s.getQuantification() == Quantification.UNIVERSAL)
                    .collect(Collectors.toList()));

            return false;
        }
    }
}