package org.resolvelite.semantics.query;

import org.resolvelite.semantics.DuplicateSymbolException;
import org.resolvelite.semantics.Scope;
import org.resolvelite.semantics.SymbolTable;
import org.resolvelite.semantics.symbol.Symbol;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
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

    @Override public List<R> searchFromContext(Scope source, SymbolTable repo)
            throws DuplicateSymbolException {
        return baseQuery.searchFromContext(source, repo).stream()
                .map(mapping::apply)
                .collect(Collectors.toList());
    }
}
