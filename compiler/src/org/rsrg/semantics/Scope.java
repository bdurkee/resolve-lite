package org.rsrg.semantics;

import org.rsrg.semantics.programtype.PTType;
import org.rsrg.semantics.query.MultimatchSymbolQuery;
import org.rsrg.semantics.query.SymbolQuery;
import org.rsrg.semantics.searchers.TableSearcher;
import org.rsrg.semantics.searchers.TableSearcher.SearchContext;
import org.rsrg.semantics.symbol.FacilitySymbol;
import org.rsrg.semantics.symbol.Symbol;

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