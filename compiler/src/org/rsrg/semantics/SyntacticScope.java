package org.rsrg.semantics;

import org.antlr.v4.runtime.ParserRuleContext;
import org.rsrg.semantics.programtype.PTType;
import org.rsrg.semantics.query.MultimatchSymbolQuery;
import org.rsrg.semantics.query.SymbolQuery;
import org.rsrg.semantics.searchers.TableSearcher;
import org.rsrg.semantics.searchers.TableSearcher.SearchContext;
import org.rsrg.semantics.symbol.FacilitySymbol;
import org.rsrg.semantics.symbol.Symbol;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class SyntacticScope extends AbstractScope {

    protected final Map<String, Symbol> symbols;
    private final SymbolTable symtab;

    protected ParserRuleContext definingTree;
    protected Scope parent;
    protected final String moduleID;

    SyntacticScope(SymbolTable scopeRepo, ParserRuleContext definingTree,
                   Scope parent, String moduleID,
                   Map<String, Symbol> bindingSyms) {
        this.symtab = scopeRepo;
        this.symbols = bindingSyms;
        this.parent = parent;
        this.moduleID = moduleID;
        this.definingTree = definingTree;
    }

    public ParserRuleContext getDefiningTree() {
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

    @Override public String toString() {
        String s = "";
        if ( definingTree != null )
            s += definingTree.getClass().getSimpleName();
        return s + symbols.keySet() + "";
    }

    @Override public <E extends Symbol> boolean addMatches(
            TableSearcher<E> searcher, List<E> matches,
            Set<Scope> searchedScopes,
            Map<String, PTType> genericInstantiations,
            FacilitySymbol instantiatingFacility, TableSearcher.SearchContext l)
            throws DuplicateSymbolException {
        boolean finished = false;

        if ( !searchedScopes.contains(this) ) {
            searchedScopes.add(this);

            Map<String, Symbol> symbolTableView = symbols;
            if ( instantiatingFacility != null ) {

                symbolTableView =
                        updateSymbols(symbols, genericInstantiations,
                                instantiatingFacility);

            }
            finished = searcher.addMatches(symbolTableView, matches, l);

            if ( !finished ) {
                finished =
                        parent.addMatches(searcher, matches, searchedScopes,
                                genericInstantiations, instantiatingFacility, l);
            }
        }
        return finished;
    }

    private Map<String, Symbol> updateSymbols(
            Map<String, Symbol> currentBindings,
            Map<String, PTType> genericInstantiations,
            FacilitySymbol instantiatingFacility) {
        Map<String, Symbol> instantiatedBindings = new LinkedHashMap<>();
        for (Symbol s : currentBindings.values()) {
            instantiatedBindings.put(s.getName(), s.instantiateGenerics(
                    genericInstantiations, instantiatingFacility));
        }
        return instantiatedBindings;
    }
}
