package org.resolvelite.semantics.query;

import org.resolvelite.semantics.DuplicateSymbolException;
import org.resolvelite.semantics.Scope;
import org.resolvelite.semantics.SymbolTable;
import org.resolvelite.semantics.symbol.Symbol;

import java.util.LinkedList;
import java.util.List;

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

    private final SymbolQuery<T> myBaseQuery;
    private final Mapping<T, R> myMapping;

    public ResultProcessingQuery(SymbolQuery<T> baseQuery,
                                 Mapping<T, R> processing) {

        myBaseQuery = baseQuery;
        myMapping = processing;
    }

    @Override
    public List<R> searchFromContext(Scope source, SymbolTable repo)
            throws DuplicateSymbolException {
        List<T> intermediateMatches =
                myBaseQuery.searchFromContext(source, repo);

        List<R> finalMatches = new LinkedList<>();
        for (T intermediateMatch : intermediateMatches) {
            finalMatches.add(myMapping.map(intermediateMatch));
        }

        return finalMatches;
    }
}
