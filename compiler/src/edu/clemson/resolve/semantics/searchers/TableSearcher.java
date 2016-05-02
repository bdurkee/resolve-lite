package edu.clemson.resolve.semantics.searchers;

import org.jetbrains.annotations.NotNull;
import edu.clemson.resolve.semantics.DuplicateSymbolException;
import edu.clemson.resolve.semantics.MathSymbolTable;
import edu.clemson.resolve.semantics.UnexpectedSymbolException;
import edu.clemson.resolve.semantics.symbol.Symbol;

import java.util.List;
import java.util.Map;

/**
 * A {@code TableSearcher} is a strategy for searching a {@link MathSymbolTable},
 * adding any {@link Symbol}s that match the search to an accumulator.
 *
 * @param <E> Permits concrete implementations of this interface to refine the
 *            type of {@code Symbol}s they will match. This searcher guarantees
 *            that any entry it matches will descend from {@code E}. Put another
 *            way: no matched entry will not be a subtype of {@code E}.
 */
public interface TableSearcher<E extends Symbol> {

    public static enum SearchContext {
        GLOBAL, SOURCE_MODULE, IMPORT, FACILITY
    }

    /**
     * Adds any symbol table entries from {@code entries} that match
     * this search to {@code matches}. The order that they are added is
     * determined by the concrete base-class.
     * <p>
     * If no matches exist, the method will simply leave {@code matches}
     * unmodified.</p>
     * <p>
     * The semantics of the incoming accumulator are only that it is the
     * appropriate place to add new matches, not that it will necessarily
     * contain all matches so far. This allows intermediate accumulators to be
     * created and passed without causing strange behavior. <em>No concrete
     * subclass should depend on the incoming value of the accumulator, save
     * that it will be non-{@code null} and mutable.</em></p>
     *
     * @param entries The symbol table entries to consider.
     * @param matches A non-{@code null} accumulator of matches.
     * @param l       The context from which {@code entries} was drawn.
     *
     * @return {@code true} if {@code matches} now represents a
     * final list of search results&mdash;i.e., no further symbol table
     * entries should be considered. {@code false} indicates that
     * the search should continue, provided there are additional
     * un-searched scopes.
     * @throws DuplicateSymbolException If more than one match is found in
     *                                  {@code entries} where no more than one was expected.
     */
    public boolean addMatches(@NotNull Map<String, Symbol> entries,
                              @NotNull List<E> matches,
                              @NotNull SearchContext l)
            throws DuplicateSymbolException, UnexpectedSymbolException;
}
