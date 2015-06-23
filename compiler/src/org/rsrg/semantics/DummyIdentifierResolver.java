package org.rsrg.semantics;

import org.rsrg.semantics.programtype.PTType;
import org.rsrg.semantics.query.MultimatchSymbolQuery;
import org.rsrg.semantics.query.SymbolQuery;
import org.rsrg.semantics.searchers.TableSearcher;
import org.rsrg.semantics.symbol.FacilitySymbol;
import org.rsrg.semantics.symbol.Symbol;

import java.util.*;

public class DummyIdentifierResolver extends AbstractScope {

    @Override public <E extends Symbol> List<E> query(
            MultimatchSymbolQuery<E> query) {
        return new LinkedList<E>();
    }

    @Override public <E extends Symbol> E queryForOne(SymbolQuery<E> query)
            throws NoSuchSymbolException,
                DuplicateSymbolException {
        throw new NoSuchSymbolException();
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

    @Override public Symbol define(Symbol s) throws DuplicateSymbolException {
        return s;
    }

    @Override public <T extends Symbol> List<T> getSymbolsOfType(Class<T> type) {
        return new ArrayList<>();
    }
}