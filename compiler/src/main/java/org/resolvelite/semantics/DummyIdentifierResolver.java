package org.resolvelite.semantics;

import edu.emory.mathcs.backport.java.util.Collections;
import org.resolvelite.semantics.programtype.PTType;
import org.resolvelite.semantics.query.MultimatchSymbolQuery;
import org.resolvelite.semantics.query.SymbolQuery;
import org.resolvelite.semantics.searchers.TableSearcher;
import org.resolvelite.semantics.symbol.FacilitySymbol;
import org.resolvelite.semantics.symbol.Symbol;

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

    @Override public <T extends Symbol> List<T> getSymbolsOfType(Class<T> type) {
        return new ArrayList<>();
    }

    @Override public Symbol define(Symbol s) throws DuplicateSymbolException {
        return s;
    }
}