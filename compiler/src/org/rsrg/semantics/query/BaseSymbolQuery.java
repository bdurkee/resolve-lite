package org.rsrg.semantics.query;

import org.rsrg.semantics.DuplicateSymbolException;
import org.rsrg.semantics.MathSymbolTableBuilder;
import org.rsrg.semantics.Scope;
import org.rsrg.semantics.ScopeSearchPath;
import org.rsrg.semantics.searchers.TableSearcher;
import org.rsrg.semantics.symbol.Symbol;

import java.util.List;

/**
 * The most basic implementation of {@link SymbolQuery SymbolQuery}, which
 * pairs a {@link ScopeSearchPath} with an {@link TableSearcher} to define a
 * fully parameterized strategy for searching a set of scopes.
 */
public class BaseSymbolQuery<E extends Symbol> implements SymbolQuery<E> {

    private final ScopeSearchPath searchPath;
    private final TableSearcher<E> searcher;

    public BaseSymbolQuery(ScopeSearchPath path, TableSearcher<E> searcher) {
        this.searchPath = path;
        this.searcher = searcher;
    }

    @Override public List<E> searchFromContext(Scope source, MathSymbolTableBuilder repo)
            throws DuplicateSymbolException {
        return searchPath.searchFromContext(searcher, source, repo);
    }
}
