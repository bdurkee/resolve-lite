package org.rsrg.semantics;

import org.rsrg.semantics.programtype.PTType;
import org.rsrg.semantics.searchers.TableSearcher;
import org.rsrg.semantics.symbol.Symbol;

import java.util.*;

public abstract class AbstractScope implements Scope {

    @Override public final <E extends Symbol> List<E> getMatches(
            TableSearcher<E> searcher, TableSearcher.SearchContext l)
                throws DuplicateSymbolException {
        List<E> result = new ArrayList<>();
        Set<Scope> searchedScopes = new HashSet<>();
        Map<String, PTType> genericInstantiations = new HashMap<>();
        addMatches(searcher, result, searchedScopes, genericInstantiations, null, l);
        return result;
    }
}
