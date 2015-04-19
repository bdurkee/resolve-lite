package org.resolvelite.semantics.searchers;

import org.resolvelite.semantics.DuplicateSymbolException;
import org.resolvelite.semantics.symbol.Symbol;

import java.util.List;
import java.util.Map;

/**
 * A simple refinement on {@link TableSearcher} that guarantees its method will
 * not throw a {@link DuplicateSymbolException}.
 */
public interface MultimatchTableSearcher<E extends Symbol>
        extends
        TableSearcher<E> {

    /**
     * Refines {@link TableSearcher#addMatches} to guarantee that it will not
     * throw a {@link DuplicateSymbolException}. Otherwise, behaves
     * identically.
     */
    @Override
    public boolean addMatches(Map<String, Symbol> entries, List<E> matches,
                              SearchContext l);
}
