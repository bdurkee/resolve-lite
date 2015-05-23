package org.resolvelite.semantics;

import org.resolvelite.semantics.programtype.PTType;
import org.resolvelite.semantics.query.MultimatchSymbolQuery;
import org.resolvelite.semantics.query.SymbolQuery;
import org.resolvelite.semantics.searchers.TableSearcher;
import org.resolvelite.semantics.searchers.TableSearcher.SearchContext;
import org.resolvelite.semantics.symbol.FacilitySymbol;
import org.resolvelite.semantics.symbol.Symbol;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface Scope {

    public <E extends Symbol> List<E> query(MultimatchSymbolQuery<E> query);

    public <E extends Symbol> E queryForOne(SymbolQuery<E> query)
            throws NoSuchSymbolException,
                DuplicateSymbolException;

    public <E extends Symbol> boolean addMatches(TableSearcher<E> searcher,
            List<E> matches, Set<Scope> searchedScopes,
            Map<String, PTType> genericInstantiations,
            FacilitySymbol instantiatingFacility, SearchContext l)
            throws DuplicateSymbolException;

    public <E extends Symbol> List<E> getMatches(TableSearcher<E> searcher,
            SearchContext l) throws DuplicateSymbolException;

    public Symbol define(Symbol s) throws DuplicateSymbolException;
}
