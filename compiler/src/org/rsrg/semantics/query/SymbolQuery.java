package org.rsrg.semantics.query;

import org.jetbrains.annotations.NotNull;
import org.rsrg.semantics.*;
import org.rsrg.semantics.symbol.Symbol;

import java.util.List;

/**
 * A {@code SymbolQuery} defines a strategy for returning a list of
 * {@link Symbol}s that meet a certain set of search criteria starting from
 * some <em>source scope</em>.
 *
 * @since 0.0.1
 */
public interface SymbolQuery<E extends Symbol> {

    /**
     * Given a source {@link Scope Scope} and a {@link MathSymbolTable} containing
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
    public List<E> searchFromContext(@NotNull Scope source,
                                     @NotNull MathSymbolTable scopeRepo)
            throws DuplicateSymbolException, NoSuchModuleException, UnexpectedSymbolException;
}
