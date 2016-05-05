package edu.clemson.resolve.semantics.query;

import edu.clemson.resolve.semantics.*;
import org.jetbrains.annotations.NotNull;
import edu.clemson.resolve.semantics.searchers.TableSearcher;
import edu.clemson.resolve.semantics.symbol.Symbol;

import java.util.List;

/**
 * The most basic implementation of {@link SymbolQuery SymbolQuery}, which pairs a {@link ScopeSearchPath} with an
 * {@link TableSearcher} to define a fully parameterized strategy for searching a set of scopes.
 */
public class BaseSymbolQuery<E extends Symbol> implements SymbolQuery<E> {

    @NotNull
    private final ScopeSearchPath searchPath;
    @NotNull
    private final TableSearcher<E> searcher;

    public BaseSymbolQuery(@NotNull ScopeSearchPath path,
                           @NotNull TableSearcher<E> searcher) {
        this.searchPath = path;
        this.searcher = searcher;
    }

    @Override
    public List<E> searchFromContext(@NotNull Scope source,
                                     @NotNull MathSymbolTable repo)
            throws DuplicateSymbolException, NoSuchModuleException, UnexpectedSymbolException {
        return searchPath.searchFromContext(searcher, source, repo);
    }
}
