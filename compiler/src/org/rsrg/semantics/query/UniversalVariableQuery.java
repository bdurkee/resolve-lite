package org.rsrg.semantics.query;

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
                        MathSymbolTableBuilder.ImportStrategy.IMPORT_NONE,
                        MathSymbolTableBuilder.FacilityStrategy.FACILITY_IGNORE, false),
                        new UniversalVariableSearcher());
    }

    @Override public List<MathSymbol> searchFromContext(Scope source,
            MathSymbolTableBuilder repo) {

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

        @Override public boolean addMatches(Map<String, Symbol> entries,
                                  List<MathSymbol> matches, SearchContext l) {

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