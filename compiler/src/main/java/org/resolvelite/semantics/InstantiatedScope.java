package org.resolvelite.semantics;

import org.resolvelite.semantics.programtype.PTType;
import org.resolvelite.semantics.query.MultimatchSymbolQuery;
import org.resolvelite.semantics.query.SymbolQuery;
import org.resolvelite.semantics.searchers.TableSearcher;
import org.resolvelite.semantics.symbol.FacilitySymbol;
import org.resolvelite.semantics.symbol.Symbol;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An {@code InstantiatedScope} decorates an existing {@link Scope Scope} such
 * that calls to {@link Scope#addMatches addMatches()}, the search method to
 * which all others defer, are augmented with an additional set of generic
 * instantiations and an instantiating facility.
 */
public class InstantiatedScope extends AbstractScope {
    @Override public <E extends Symbol> List<E> query(
            MultimatchSymbolQuery<E> query) {
        return null;
    }

    @Override public <E extends Symbol> E queryForOne(SymbolQuery<E> query)
            throws NoSuchSymbolException,
                DuplicateSymbolException {
        return null;
    }

    @Override public <E extends Symbol> boolean
            addMatches(TableSearcher<E> searcher, List<E> matches,
                    Set<Scope> searchedScopes,
                    Map<String, PTType> genericInstantiations,
                    FacilitySymbol instantiatingFacility,
                    TableSearcher.SearchContext l)
                    throws DuplicateSymbolException {
        return false;
    }

    @Override public <T extends Symbol> List<T> getSymbolsOfType(Class<T> type) {
        return null;
    }

    @Override public Symbol define(Symbol s) throws DuplicateSymbolException {
        return null;
    }
}
