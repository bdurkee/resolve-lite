package org.rsrg.semantics.query;

import org.jetbrains.annotations.NotNull;
import org.rsrg.semantics.*;
import org.rsrg.semantics.symbol.Symbol;

import java.util.List;

public interface MultimatchSymbolQuery<E extends Symbol>
        extends
            SymbolQuery<E> {

    /** Behaves just as {@link SymbolQuery#searchFromContext(Scope, MathSymbolTable)},
     *  except that it cannot throw a {@link DuplicateSymbolException}.
     */
    @Override public List<E> searchFromContext(@NotNull Scope source,
                                               @NotNull MathSymbolTable repo)
            throws NoSuchModuleException, UnexpectedSymbolException;
}
