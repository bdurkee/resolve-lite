package edu.clemson.resolve.semantics;

import org.jetbrains.annotations.NotNull;
import edu.clemson.resolve.semantics.searchers.TableSearcher;
import edu.clemson.resolve.semantics.symbol.Symbol;

import java.util.List;

public interface ScopeSearchPath {

    /** Applies the given {@link TableSearcher} to the appropriate {@link Scope}s,
     *  given a source scope and a {@link MathSymbolTable} containing
     *  any imports, returning a list of matching {@link Symbol}s.
     *  <p>
     *  If there are no matches, returns an empty list. If more than one match is
     *  found and {@code searcher} expects no more than one match, throws a
     *  {@link DuplicateSymbolException}.</p>
     * 
     *  @param searcher A {@link TableSearcher} to apply to each scope along
     *        the search path.
     *  @param source The current scope from which the search was spawned.
     *  @param repo A symbol table containing any referenced modules.
     *
     *  @throws DuplicateSymbolException if two or more symbols are matched
     *  @throws NoSuchModuleException if, in the process of searching, a non
     *  extant module is referenced
     *
     *  @return A list of matches
     */
    @NotNull public <E extends Symbol> List<E> searchFromContext(
            @NotNull TableSearcher<E> searcher, @NotNull Scope source,
            @NotNull MathSymbolTable repo)
            throws DuplicateSymbolException, NoSuchModuleException, UnexpectedSymbolException;
}