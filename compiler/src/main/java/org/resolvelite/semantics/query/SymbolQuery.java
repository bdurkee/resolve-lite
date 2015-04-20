package org.resolvelite.semantics.query;

import org.resolvelite.semantics.DuplicateSymbolException;
import org.resolvelite.semantics.Scope;
import org.resolvelite.semantics.SymbolTable;
import org.resolvelite.semantics.symbol.Symbol;

import java.util.List;

/**
 * A {@code SymbolQuery} defines a strategy for returning a list of
 * {@link Symbol}s that meet a certain set of search criteria starting from
 * some <em>source scope</em>.
 */
public interface SymbolQuery<E extends Symbol> {

    /**
     * Given a source {@link Scope Scope} and a {@link SymbolTable} containing
     * any imports, from which {@code source} draws, searches them
     * appropriately, returning a list of matching {@link Symbol}s
     * that are subtypes of {@code E}.
     * 
     * If there are no matches, returns an empty list. If more than one match is
     * found where no more than one was expected, throws a
     * {@link DuplicateSymbolException}.
     * 
     * @param source The source scope from which the search was spawned.
     * @param scopeRepo A repository of any referenced modules.
     * 
     * @return A list of matches.
     */
    public List<E> searchFromContext(Scope source, SymbolTable scopeRepo)
            throws DuplicateSymbolException;
}
