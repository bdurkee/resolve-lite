package org.resolvelite.semantics;

import org.antlr.v4.runtime.tree.ParseTree;
import org.resolvelite.semantics.programtype.PTType;
import org.resolvelite.semantics.query.MultimatchSymbolQuery;
import org.resolvelite.semantics.query.SymbolQuery;
import org.resolvelite.semantics.searchers.TableSearcher;
import org.resolvelite.semantics.searchers.TableSearcher.SearchContext;
import org.resolvelite.semantics.symbol.FacilitySymbol;
import org.resolvelite.semantics.symbol.Symbol;
import org.resolvelite.typereasoning.TypeGraph;

import java.util.*;
import java.util.stream.Collectors;

public abstract class SyntacticScope extends AbstractScope {

    protected final Map<String, Symbol> symbols;
    private final SymbolTable symtab;

    protected ParseTree definingTree;
    protected Scope parent;
    protected final String moduleID;

    SyntacticScope(SymbolTable scopeRepo, ParseTree definingTree, Scope parent,
            String moduleID, Map<String, Symbol> bindingSyms) {
        this.symtab = scopeRepo;
        this.symbols = bindingSyms;
        this.parent = parent;
        this.moduleID = moduleID;
        this.definingTree = definingTree;
    }

    public ParseTree getDefiningTree() {
        return definingTree;
    }

    public String getModuleID() {
        return moduleID;
    }

    @Override public Symbol define(Symbol s) throws DuplicateSymbolException {
        if ( symbols.containsKey(s.getName()) ) {
            throw new DuplicateSymbolException(symbols.get(s.getName()));
        }
        symbols.put(s.getName(), s);
        return s;
    }

    @Override public <E extends Symbol> List<E> query(
            MultimatchSymbolQuery<E> query) {
        return query.searchFromContext(this, symtab);
    }

    @Override public <E extends Symbol> E queryForOne(SymbolQuery<E> query)
            throws NoSuchSymbolException,
                DuplicateSymbolException {
        List<E> results = query.searchFromContext(this, symtab);
        if ( results.isEmpty() )
            throw new NoSuchSymbolException();
        else if ( results.size() > 1 ) throw new DuplicateSymbolException();
        return results.get(0);
    }

    @Override public <T extends Symbol> List<T> getSymbolsOfType(Class<T> type) {
        return symbols.values().stream()
                .filter(type::isInstance)
                .map(type::cast)
                .collect(Collectors.toList());
    }

    @Override public String toString() {
        return symbols.keySet() + "";
    }

    @Override public <E extends Symbol> boolean addMatches(
            TableSearcher<E> searcher, List<E> matches,
            Set<Scope> searchedScopes,
            Map<String, PTType> genericInstantiations,
            FacilitySymbol instantiatingFacility, SearchContext l)
            throws DuplicateSymbolException {
        boolean finished = false;

        if ( !searchedScopes.contains(this) ) {
            searchedScopes.add(this);

            Map<String, Symbol> symbolTableView = symbols;
            /*if (instantiatingFacility != null) {
                symbolTableView =
                        new InstantiatedSymbolTable(myBindings,
                                genericInstantiations, instantiatingFacility);
            }*/
            finished = searcher.addMatches(symbolTableView, matches, l);

            if ( !finished ) {
                finished =
                        parent.addMatches(searcher, matches, searchedScopes,
                                genericInstantiations, instantiatingFacility, l);
            }
        }
        return finished;
    }
}
