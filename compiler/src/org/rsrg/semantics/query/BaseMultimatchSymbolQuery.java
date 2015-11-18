package org.rsrg.semantics.query;

import org.rsrg.semantics.DuplicateSymbolException;
import org.rsrg.semantics.Scope;
import org.rsrg.semantics.ScopeSearchPath;
import org.rsrg.semantics.MathSymbolTableBuilder;
import org.rsrg.semantics.searchers.MultimatchTableSearcher;
import org.rsrg.semantics.symbol.Symbol;

import java.util.List;

/**
 * Refines {@link BaseSymbolQuery} by guaranteeing that the associated searcher
 * is a {@link MultimatchTableSearcher},
 * and thus the search methods of this class are guaranteed not to throw a
 * {@link DuplicateSymbolException DuplicateSymbolException}.</p>
 */
public class BaseMultimatchSymbolQuery<E extends Symbol>
        extends
            BaseSymbolQuery<E> {

    public BaseMultimatchSymbolQuery(ScopeSearchPath path,
                                     MultimatchTableSearcher<E> searcher) {
        super(path, searcher);
    }

    /**
     * Refines {@link BaseSymbolQuery#searchFromContext} to guarantee that it
     * will not throw a {@link DuplicateSymbolException}.
     * Otherwise, behaves identically.
     */
    @Override public List<E> searchFromContext(Scope source, MathSymbolTableBuilder repo) {
        List<E> result;
        try {
            result = super.searchFromContext(source, repo);
        }
        catch (DuplicateSymbolException dse) {
            //Not possible.  We know our searcher is, in fact, a
            //MultimatchTableSearch
            throw new RuntimeException(dse);
        }

        return result;
    }
}
