package org.rsrg.semantics.query;

import org.jetbrains.annotations.NotNull;
import org.rsrg.semantics.*;
import org.rsrg.semantics.searchers.MultimatchTableSearcher;
import org.rsrg.semantics.symbol.Symbol;

import java.util.List;

/** Refines {@link BaseSymbolQuery} by guaranteeing that the associated searcher
 *  is a {@link MultimatchTableSearcher},
 *  and thus the search methods of this class are guaranteed not to throw a
 *  {@link DuplicateSymbolException DuplicateSymbolException}.</p>
 */
public class BaseMultimatchSymbolQuery<E extends Symbol>
        extends
            BaseSymbolQuery<E> {

    public BaseMultimatchSymbolQuery(@NotNull ScopeSearchPath path,
                                     @NotNull MultimatchTableSearcher<E> searcher) {
        super(path, searcher);
    }

    /** Refines {@link BaseSymbolQuery#searchFromContext} to guarantee that it
     *  will not throw a {@link DuplicateSymbolException}.
     *  Otherwise, behaves identically.
     */
    @Override public List<E> searchFromContext(@NotNull Scope source,
                                               @NotNull MathSymbolTable repo)
            throws NoSuchModuleException, UnexpectedSymbolException {
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
