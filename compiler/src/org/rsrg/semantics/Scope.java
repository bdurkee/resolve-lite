package org.rsrg.semantics;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rsrg.semantics.programtype.ProgType;
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

    @NotNull public <E extends Symbol> List<E> query(
            @NotNull MultimatchSymbolQuery<E> query)
            throws NoSuchModuleException, UnexpectedSymbolException;

    @NotNull public <E extends Symbol> E queryForOne(
            @NotNull SymbolQuery<E> query) throws NoSuchSymbolException,
            DuplicateSymbolException, NoSuchModuleException, UnexpectedSymbolException;

    public <E extends Symbol> boolean addMatches(
            @NotNull TableSearcher<E> searcher,
            @NotNull List<E> matches, @NotNull Set<Scope> searchedScopes,
            @NotNull Map<ProgType, ProgType> genericInstantiations,
            @Nullable FacilitySymbol instantiatingFacility,
            @NotNull SearchContext l)
            throws DuplicateSymbolException, UnexpectedSymbolException;

    @NotNull public <E extends Symbol> List<E> getMatches(
            @NotNull TableSearcher<E> searcher, @NotNull SearchContext l)
            throws DuplicateSymbolException, UnexpectedSymbolException;

    @NotNull public Symbol define(@NotNull Symbol s)
            throws DuplicateSymbolException;

    @NotNull public <T extends Symbol> List<T> getSymbolsOfType(
            @NotNull Class<T> type);

    @NotNull public List<Symbol> getSymbolsOfType(@NotNull Class<?> ... types);

}