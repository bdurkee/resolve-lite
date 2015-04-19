package org.resolvelite.semantics.query;

import org.resolvelite.semantics.DuplicateSymbolException;
import org.resolvelite.semantics.Scope;
import org.resolvelite.semantics.ScopeSearchPath;
import org.resolvelite.semantics.SymbolTable;
import org.resolvelite.semantics.searchers.MultimatchTableSearcher;
import org.resolvelite.semantics.symbol.Symbol;

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
    @Override
    public List<E> searchFromContext(Scope source, SymbolTable repo) {
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
