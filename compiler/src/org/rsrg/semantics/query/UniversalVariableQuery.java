package org.rsrg.semantics.query;

import org.jetbrains.annotations.NotNull;
import org.rsrg.semantics.*;
import org.rsrg.semantics.MathSymbolTable.FacilityStrategy;
import org.rsrg.semantics.MathSymbolTable.ImportStrategy;
import org.rsrg.semantics.searchers.MultimatchTableSearcher;
import org.rsrg.semantics.symbol.MathSymbol;
import org.rsrg.semantics.symbol.Symbol;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.rsrg.semantics.MathSymbolTable.FacilityStrategy.FACILITY_IGNORE;
import static org.rsrg.semantics.MathSymbolTable.ImportStrategy.IMPORT_NONE;

public class UniversalVariableQuery
        implements
            MultimatchSymbolQuery<MathSymbol> {

    public static final MultimatchSymbolQuery<MathSymbol> INSTANCE =
            (MultimatchSymbolQuery<MathSymbol>) new UniversalVariableQuery();

    @NotNull private final BaseSymbolQuery<MathSymbol> baseQuery;

    private UniversalVariableQuery() {
        this.baseQuery =
                new BaseSymbolQuery<MathSymbol>(new UnqualifiedPath(
                        IMPORT_NONE, FACILITY_IGNORE, false),
                        new UniversalVariableSearcher());
    }

    @Override public List<MathSymbol> searchFromContext(@NotNull Scope source,
                                                        @NotNull MathSymbolTable repo)
            throws NoSuchModuleException {
        List<MathSymbol> result;
        try {
            result = baseQuery.searchFromContext(source, repo);
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

        @Override public boolean addMatches(
                @NotNull Map<String, Symbol> entries,
                @NotNull List<MathSymbol> matches,
                @NotNull SearchContext l) {

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