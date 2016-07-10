package edu.clemson.resolve.semantics.query;

import edu.clemson.resolve.semantics.*;
import org.jetbrains.annotations.NotNull;
import edu.clemson.resolve.semantics.symbol.Symbol;

import java.util.List;

public interface MultimatchSymbolQuery<E extends Symbol> extends SymbolQuery<E> {

    /**
     * Behaves just as {@link SymbolQuery#searchFromContext(Scope, MathSymbolTable)}, except that it cannot throw
     * a {@link DuplicateSymbolException}.
     */
    @Override
    public List<E> searchFromContext(@NotNull Scope source, @NotNull MathSymbolTable repo)
            throws NoSuchModuleException, UnexpectedSymbolException;
}
