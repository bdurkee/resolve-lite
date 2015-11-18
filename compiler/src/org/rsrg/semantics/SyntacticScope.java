package org.rsrg.semantics;

import edu.clemson.resolve.compiler.ErrorKind;
import org.antlr.v4.runtime.ParserRuleContext;
import org.rsrg.semantics.programtype.PTType;
import org.rsrg.semantics.query.MultimatchSymbolQuery;
import org.rsrg.semantics.query.SymbolQuery;
import org.rsrg.semantics.searchers.TableSearcher;
import org.rsrg.semantics.symbol.FacilitySymbol;
import org.rsrg.semantics.symbol.Symbol;

import java.util.*;
import java.util.stream.Collectors;

public abstract class SyntacticScope extends AbstractScope {

    protected final Map<String, Symbol> symbols;
    private final MathSymbolTableBuilder symtab;

    protected ParserRuleContext definingTree;
    protected Scope parent;
    protected final String moduleID;

    SyntacticScope(MathSymbolTableBuilder scopeRepo, ParserRuleContext definingTree,
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
        //TODO: bubble me over to the populator so I can be nicely printed.
        /*if ( s.getName().equals(moduleID) ) {

            symtab.getCompiler().errMgr.semanticError(
                    ErrorKind.SYMBOL_NAME_MATCHES_MODULE_NAME,
                    s.getDefiningTree().getStart(), s.toString(),
                    s.getSymbolDescription());
            return s;
        }*/
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



    @Override public <T extends Symbol> List<T> getSymbolsOfType(Class<T> type) {
        return symbols.values().stream()
                .filter(type::isInstance)
                .map(type::cast)
                .collect(Collectors.toList());
    }

    public List<Symbol> getSymbolsOfType(Class<?> ... types) {
        List<Symbol> result = new ArrayList<>();
        for (Symbol s : symbols.values()) {
            for (Class<?> t : types) {
                if (t.isInstance(s)) result.add(s);
            }
        }
        return result;
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
