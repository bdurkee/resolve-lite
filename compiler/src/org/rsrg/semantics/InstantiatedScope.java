package org.rsrg.semantics;

import org.jetbrains.annotations.NotNull;
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

/** An {@code InstantiatedScope} decorates an existing {@link Scope Scope} such
 *  that calls to {@link Scope#addMatches addMatches()}, the search method to
 *  which all others defer, are augmented with an additional set of generic
 *  instantiations and an instantiating facility.
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

    @NotNull @Override public <E extends Symbol> List<E> query(
            @NotNull MultimatchSymbolQuery<E> query)
            throws NoSuchModuleException, UnexpectedSymbolException {
        return myBaseScope.query(query);
    }

    @NotNull @Override public <E extends Symbol> E queryForOne(
            @NotNull SymbolQuery<E> query)
            throws NoSuchSymbolException,
            DuplicateSymbolException, NoSuchModuleException,
            UnexpectedSymbolException {
        return myBaseScope.queryForOne(query);
    }

    @Override public <E extends Symbol> boolean
            addMatches(@NotNull TableSearcher<E> searcher, @NotNull List<E> matches,
                    @NotNull Set<Scope> searchedScopes,
                    @NotNull Map<String, PTType> genericInstantiations,
                    FacilitySymbol facilityInstantiation,
                    @NotNull TableSearcher.SearchContext l)
            throws DuplicateSymbolException, UnexpectedSymbolException {

        if ( facilityInstantiation != null ) {
            //It's unclear how this could happen or what it would mean, so we
            //fail fast.  If an example triggers this, we need to think
            //carefully about what it would mean.
            throw new RuntimeException("Duplicate instantiation???");
        }

        return myBaseScope.addMatches(searcher, matches, searchedScopes,
                myAdditionalGenericInstantiations, myInstantiatingFacility, l);
    }

    @NotNull @Override public <T extends Symbol> List<T> getSymbolsOfType(@NotNull Class<T> type) {
        return myBaseScope.getSymbolsOfType(type);
    }

    @NotNull @Override public List<Symbol> getSymbolsOfType(@NotNull Class<?>... type) {
        return myBaseScope.getSymbolsOfType(type);
    }

    @NotNull @Override public Symbol define(@NotNull Symbol s) throws DuplicateSymbolException {
        return myBaseScope.define(s);
    }

}
