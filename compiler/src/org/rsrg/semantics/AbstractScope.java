package org.rsrg.semantics;

import org.jetbrains.annotations.NotNull;
import org.rsrg.semantics.programtype.ProgType;
import org.rsrg.semantics.searchers.TableSearcher;
import org.rsrg.semantics.symbol.Symbol;

import java.util.*;

public abstract class AbstractScope implements Scope {

    @NotNull @Override public final <E extends Symbol> List<E> getMatches(
            @NotNull TableSearcher<E> searcher,
            @NotNull TableSearcher.SearchContext l)
            throws DuplicateSymbolException, UnexpectedSymbolException {
        List<E> result = new ArrayList<>();
        Set<Scope> searchedScopes = new HashSet<>();
        Map<String, ProgType> genericInstantiations = new HashMap<>();
        addMatches(searcher, result, searchedScopes,
                genericInstantiations, null, l);
        return result;
    }
}
