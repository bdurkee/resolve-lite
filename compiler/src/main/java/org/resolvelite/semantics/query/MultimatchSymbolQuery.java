package org.resolvelite.semantics.query;

import org.resolvelite.semantics.DuplicateSymbolException;
import org.resolvelite.semantics.Scope;
import org.resolvelite.semantics.SymbolTable;
import org.resolvelite.semantics.symbol.Symbol;

import java.util.List;

public interface MultimatchSymbolQuery<E extends Symbol> extends SymbolQuery<E> {

    /**
     * Behaves just as {@link SymbolQuery#searchFromContext}, except that it
     * cannot throw a {@link DuplicateSymbolException}.
     */
    @Override public List<E> searchFromContext(Scope source, SymbolTable repo);
}
