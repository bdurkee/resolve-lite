package org.resolvelite.semantics;

import org.resolvelite.semantics.searchers.TableSearcher;
import org.resolvelite.semantics.symbol.Symbol;

import java.util.List;

public interface ScopeSearchPath {

    /**
     * Applies the given {@link TableSearcher} to the appropriate {@link Scope}
     * s, given a source scope and a {@link SymbolTable} containing
     * any imports, returning a list of matching {@link Symbol}s.
     * 
     * <p>
     * If there are no matches, returns an empty list. If more than one match is
     * found and {@code searcher} expects no more than one match, throws a
     * {@link DuplicateSymbolException}.
     * 
     * @param searcher A {@link TableSearcher} to apply to each scope along
     *        the search path.
     * @param source The current scope from which the search was spawned.
     * @param repo A symbol table containing any referenced modules.
     *
     * @return A list of matches.
     */
    public <E extends Symbol> List<E> searchFromContext(
            TableSearcher<E> searcher, Scope source, SymbolTable repo)
            throws DuplicateSymbolException,
                UnexpectedSymbolException;
}