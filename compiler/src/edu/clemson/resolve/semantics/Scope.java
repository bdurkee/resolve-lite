package edu.clemson.resolve.semantics;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import edu.clemson.resolve.semantics.programtype.ProgType;
import edu.clemson.resolve.semantics.query.MultimatchSymbolQuery;
import edu.clemson.resolve.semantics.query.SymbolQuery;
import edu.clemson.resolve.semantics.searchers.TableSearcher;
import edu.clemson.resolve.semantics.searchers.TableSearcher.SearchContext;
import edu.clemson.resolve.semantics.symbol.FacilitySymbol;
import edu.clemson.resolve.semantics.symbol.Symbol;

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
            @NotNull Map<String, ProgType> genericInstantiations,
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