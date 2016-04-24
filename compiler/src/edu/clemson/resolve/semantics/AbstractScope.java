package edu.clemson.resolve.semantics;

import org.jetbrains.annotations.NotNull;
import edu.clemson.resolve.semantics.programtype.ProgType;
import edu.clemson.resolve.semantics.searchers.TableSearcher;
import edu.clemson.resolve.semantics.symbol.Symbol;

import java.util.*;

public abstract class AbstractScope implements Scope {

    @NotNull
    @Override
    public final <E extends Symbol> List<E> getMatches(
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
