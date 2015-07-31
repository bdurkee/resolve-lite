package org.rsrg.semantics;

import org.rsrg.semantics.programtype.PTType;
import org.rsrg.semantics.query.MultimatchSymbolQuery;
import org.rsrg.semantics.query.SymbolQuery;
import org.rsrg.semantics.searchers.TableSearcher;
import org.rsrg.semantics.symbol.FacilitySymbol;
import org.rsrg.semantics.symbol.Symbol;

import java.util.HashMap;
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

    private final Scope myBaseScope;
    private final FacilitySymbol myInstantiatingFacility;
    private final Map<String, PTType> myAdditionalGenericInstantiations =
            new HashMap<>();

    public InstantiatedScope(Scope baseScope,
                             Map<String, PTType> genericInstantiations,
                             FacilitySymbol instantiatingFacility) {
        myBaseScope = baseScope;
        myAdditionalGenericInstantiations.putAll(genericInstantiations);
        myInstantiatingFacility = instantiatingFacility;
    }

    @Override public <E extends Symbol> List<E> query(
            MultimatchSymbolQuery<E> query) {
        return myBaseScope.query(query);
    }

    @Override public <E extends Symbol> E queryForOne(SymbolQuery<E> query)
            throws NoSuchSymbolException,
                DuplicateSymbolException {
        return myBaseScope.queryForOne(query);
    }

    @Override public <E extends Symbol> boolean
            addMatches(TableSearcher<E> searcher, List<E> matches,
                    Set<Scope> searchedScopes,
                    Map<String, PTType> genericInstantiations,
                    FacilitySymbol facilityInstantiation,
                    TableSearcher.SearchContext l)
                    throws DuplicateSymbolException {

        if ( facilityInstantiation != null ) {
            //It's unclear how this could happen or what it would mean, so we
            //fail fast.  If an example triggers this, we need to think
            //carefully about what it would mean.
            throw new RuntimeException("Duplicate instantiation???");
        }

        return myBaseScope.addMatches(searcher, matches, searchedScopes,
                myAdditionalGenericInstantiations, myInstantiatingFacility, l);
    }

    @Override public <T extends Symbol> List<T> getSymbolsOfType(Class<T> type) {
        return myBaseScope.getSymbolsOfType(type);
    }

    @Override public List<Symbol> getSymbolsOfType(Class<?>... type) {
        return myBaseScope.getSymbolsOfType(type);
    }

    @Override public Symbol define(Symbol s) throws DuplicateSymbolException {
        return myBaseScope.define(s);
    }

}
