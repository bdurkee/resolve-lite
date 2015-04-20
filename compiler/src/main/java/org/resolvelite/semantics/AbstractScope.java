package org.resolvelite.semantics;

import org.resolvelite.semantics.programtype.PTType;
import org.resolvelite.semantics.searchers.TableSearcher;
import org.resolvelite.semantics.symbol.Symbol;

import java.util.*;

public abstract class AbstractScope implements Scope {
    @Override public final <E extends Symbol> List<E> getMatches(
            TableSearcher<E> searcher, TableSearcher.SearchContext l)
            throws DuplicateSymbolException {
        List<E> result = new ArrayList<>();
        Set<Scope> searchedScopes = new HashSet<>();
        Map<String, PTType> genericInstantiations = new HashMap<>();
        addMatches(searcher, result, searchedScopes, genericInstantiations,
                null, l);
        return result;
    }
}
