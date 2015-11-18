package org.rsrg.semantics.query;

import org.rsrg.semantics.DuplicateSymbolException;
import org.rsrg.semantics.MathSymbolTableBuilder;
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

    private final SymbolQuery<T> baseQuery;
    private final Function<T, R> mapping;

    public ResultProcessingQuery(SymbolQuery<T> baseQuery,
                                 Function<T, R> mapping) {
        this.baseQuery = baseQuery;
        this.mapping = mapping;
    }

    @Override public List<R> searchFromContext(Scope source, MathSymbolTableBuilder repo)
            throws DuplicateSymbolException {
        List<R> processedList = baseQuery.searchFromContext(source, repo).stream()
                .map(mapping::apply)
                .collect(Collectors.toList());
        return processedList ;
    }
}
