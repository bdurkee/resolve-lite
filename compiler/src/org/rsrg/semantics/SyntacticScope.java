package org.rsrg.semantics;

import org.antlr.v4.runtime.ParserRuleContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rsrg.semantics.programtype.ProgType;
import org.rsrg.semantics.query.MultimatchSymbolQuery;
import org.rsrg.semantics.query.SymbolQuery;
import org.rsrg.semantics.searchers.TableSearcher;
import org.rsrg.semantics.symbol.FacilitySymbol;
import org.rsrg.semantics.symbol.Symbol;

import java.util.*;
import java.util.stream.Collectors;

public abstract class SyntacticScope extends AbstractScope {

    @NotNull protected final Map<String, Symbol> symbols;
    @NotNull private final MathSymbolTable symtab;

    @NotNull protected Scope parent;
    @NotNull protected final ModuleIdentifier moduleIdentifier;
    @Nullable protected ParserRuleContext definingTree;

    SyntacticScope(@NotNull MathSymbolTable scopeRepo,
                   @Nullable ParserRuleContext definingTree,
                   @NotNull Scope parent,
                   @NotNull ModuleIdentifier moduleIdentifier,
                   @NotNull Map<String, Symbol> bindingSyms) {
        this.symtab = scopeRepo;
        this.symbols = bindingSyms;
        this.parent = parent;
        this.moduleIdentifier = moduleIdentifier;
        this.definingTree = definingTree;
    }

    @Nullable public ParserRuleContext getDefiningTree() {
        return definingTree;
    }

    @NotNull public ModuleIdentifier getModuleIdentifier() {
        return moduleIdentifier;
    }

    @NotNull @Override public Symbol define(@NotNull Symbol s)
            throws DuplicateSymbolException {
        if (symbols.containsKey(s.getName())) {
            throw new DuplicateSymbolException(symbols.get(s.getName()));
        }
        //TODO: bubble me over to the populator so I can be nicely printed.
        /*if ( s.getNameToken().equals(moduleIdentifier) ) {

            symtab.getCompiler().errMgr.semanticError(
                    ErrorKind.SYMBOL_NAME_MATCHES_MODULE_NAME,
                    s.getDefiningTree().getStart(), s.toString(),
                    s.getSymbolDescription());
            return s;
        }*/
        symbols.put(s.getName(), s);
        return s;
    }

    @NotNull @Override public <E extends Symbol> List<E> query(
            @NotNull MultimatchSymbolQuery<E> query)
            throws NoSuchModuleException, UnexpectedSymbolException {
        return query.searchFromContext(this, symtab);
    }

    @NotNull @Override public <E extends Symbol> E queryForOne(
            @NotNull SymbolQuery<E> query)
            throws NoSuchSymbolException,
            DuplicateSymbolException,
            NoSuchModuleException,
            UnexpectedSymbolException {
        List<E> results = query.searchFromContext(this, symtab);
        if (results.isEmpty()) throw new NoSuchSymbolException();
        else if (results.size() > 1) throw new DuplicateSymbolException();
        return results.get(0);
    }

    @NotNull @Override public String toString() {
        String s = "";
        if (definingTree != null) {
            s = definingTree.getClass().getSimpleName();
        }
        return s + symbols.keySet() + "";
    }

    @Override public <E extends Symbol> boolean addMatches(
            @NotNull TableSearcher<E> searcher, @NotNull List<E> matches,
            @NotNull Set<Scope> searchedScopes,
            @NotNull Map<String, ProgType> genericInstantiations,
            @Nullable FacilitySymbol instantiatingFacility,
            @NotNull TableSearcher.SearchContext l)
            throws DuplicateSymbolException,
            UnexpectedSymbolException {
        boolean finished = false;

        if (!searchedScopes.contains(this)) {
            searchedScopes.add(this);

            Map<String, Symbol> symbolTableView = symbols;
            if (instantiatingFacility != null) {

                symbolTableView =
                        updateSymbols(symbols, genericInstantiations,
                                instantiatingFacility);

            }
            finished = searcher.addMatches(symbolTableView, matches, l);

            if (!finished) {
                finished =
                        parent.addMatches(searcher, matches, searchedScopes,
                                genericInstantiations, instantiatingFacility, l);
            }
        }
        return finished;
    }

    @NotNull @Override public <T extends Symbol> List<T> getSymbolsOfType(
            @NotNull Class<T> type) {
        return symbols.values().stream()
                .filter(type::isInstance)
                .map(type::cast)
                .collect(Collectors.toList());
    }

    @NotNull public List<Symbol> getSymbolsOfType(@NotNull Class<?> ... types) {
        List<Symbol> result = new ArrayList<>();
        for (Symbol s : symbols.values()) {
            for (Class<?> t : types) {
                if (t.isInstance(s)) result.add(s);
            }
        }
        return result;
    }

    @NotNull private Map<String, Symbol> updateSymbols(
            @NotNull Map<String, Symbol> currentBindings,
            @NotNull Map<String, ProgType> genericInstantiations,
            @Nullable FacilitySymbol instantiatingFacility) {
        Map<String, Symbol> instantiatedBindings = new LinkedHashMap<>();

        for (Symbol s : currentBindings.values()) {
            instantiatedBindings.put(s.getName(), s.instantiateGenerics(
                    genericInstantiations, instantiatingFacility));
        }
        return instantiatedBindings;
    }
}
