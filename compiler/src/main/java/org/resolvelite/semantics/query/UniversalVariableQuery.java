package org.resolvelite.semantics.query;

import org.resolvelite.semantics.*;
import org.resolvelite.semantics.searchers.MultimatchTableSearcher;
import org.resolvelite.semantics.symbol.MathSymbol;
import org.resolvelite.semantics.symbol.Symbol;

import java.util.ArrayList;
import java.util.Iterator;
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
                        SymbolTable.ImportStrategy.IMPORT_NONE,
                        SymbolTable.FacilityStrategy.FACILITY_IGNORE, false),
                        new UniversalVariableSearcher());
    }

    @Override public List<MathSymbol> searchFromContext(Scope source,
            SymbolTable repo) {

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