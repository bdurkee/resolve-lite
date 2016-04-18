package org.rsrg.semantics.query;

import org.jetbrains.annotations.NotNull;
import org.rsrg.semantics.*;
import org.rsrg.semantics.searchers.MultimatchTableSearcher;
import org.rsrg.semantics.symbol.MathClssftnWrappingSymbol;
import org.rsrg.semantics.symbol.Symbol;

import java.util.List;
import java.util.Map;

import static org.rsrg.semantics.MathSymbolTable.FacilityStrategy.FACILITY_IGNORE;
import static org.rsrg.semantics.MathSymbolTable.ImportStrategy.IMPORT_NONE;

public class UniversalVariableQuery
        implements
            MultimatchSymbolQuery<MathClssftnWrappingSymbol> {

    public static final MultimatchSymbolQuery<MathClssftnWrappingSymbol> INSTANCE =
            (MultimatchSymbolQuery<MathClssftnWrappingSymbol>) new UniversalVariableQuery();

    @NotNull private final BaseSymbolQuery<MathClssftnWrappingSymbol> baseQuery;

    private UniversalVariableQuery() {
        this.baseQuery =
                new BaseSymbolQuery<MathClssftnWrappingSymbol>(new UnqualifiedPath(
                        IMPORT_NONE, FACILITY_IGNORE, false),
                        new UniversalVariableSearcher());
    }

    @Override public List<MathClssftnWrappingSymbol> searchFromContext(@NotNull Scope source,
                                                                       @NotNull MathSymbolTable repo)
            throws NoSuchModuleException, UnexpectedSymbolException {
        List<MathClssftnWrappingSymbol> result;
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
                MultimatchTableSearcher<MathClssftnWrappingSymbol> {

        @Override public boolean addMatches(
                @NotNull Map<String, Symbol> entries,
                @NotNull List<MathClssftnWrappingSymbol> matches,
                @NotNull SearchContext l) throws UnexpectedSymbolException {

            for (Symbol symbol : entries.values()) {
                if (symbol instanceof MathClssftnWrappingSymbol &&
                        ((MathClssftnWrappingSymbol) symbol).getQuantification() ==
                                Quantification.UNIVERSAL) {
                    matches.add(symbol.toMathSymbol());
                }
            }
            return false;
        }
    }
}