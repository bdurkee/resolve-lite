package org.rsrg.semantics.query;

import org.jetbrains.annotations.NotNull;
import org.rsrg.semantics.DuplicateSymbolException;
import org.rsrg.semantics.MathSymbolTable;
import org.rsrg.semantics.NoSuchModuleException;
import org.rsrg.semantics.Scope;
import org.rsrg.semantics.symbol.Symbol;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * An implementation of {@link SymbolQuery SymbolQuery} that decorates an
 * existing {@link SymbolQuery}, post processing its results and returning
 * the processed set of results.
 * 
 * @param <T> The return type of the base {@link SymbolQuery}.
 * @param <R> The return type of the resultant, processed entries.
 */
public class ResultProcessingQuery<T extends Symbol, R extends Symbol>
        implements
            SymbolQuery<R> {

    @NotNull private final SymbolQuery<T> baseQuery;
    @NotNull private final Function<T, R> mapping;

    public ResultProcessingQuery(@NotNull SymbolQuery<T> baseQuery,
                                 @NotNull Function<T, R> mapping) {
        this.baseQuery = baseQuery;
        this.mapping = mapping;
    }

    @Override public List<R> searchFromContext(@NotNull Scope source,
                                               @NotNull MathSymbolTable repo)
            throws DuplicateSymbolException, NoSuchModuleException {
        return baseQuery.searchFromContext(source, repo).stream()
                .map(mapping::apply)
                .collect(Collectors.toList());
    }
}
