package edu.clemson.resolve.semantics.searchers;

import org.jetbrains.annotations.NotNull;
import edu.clemson.resolve.semantics.DuplicateSymbolException;
import edu.clemson.resolve.semantics.UnexpectedSymbolException;
import edu.clemson.resolve.semantics.symbol.Symbol;

import java.util.List;
import java.util.Map;

/**
 * A simple refinement on {@link TableSearcher} that guarantees its method will not throw a
 * {@link DuplicateSymbolException}.
 *
 * @author hamptos
 */
public interface MultimatchTableSearcher<E extends Symbol> extends TableSearcher<E> {

    /**
     * Refines {@link TableSearcher#addMatches} to guarantee that it will not throw a {@link DuplicateSymbolException}.
     * Otherwise, behaves identically.
     */
    @Override
    public boolean addMatches(@NotNull Map<String, Symbol> entries,
                              @NotNull List<E> matches,
                              @NotNull SearchContext l) throws UnexpectedSymbolException;
}
