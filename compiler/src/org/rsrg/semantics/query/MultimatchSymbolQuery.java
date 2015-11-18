package org.rsrg.semantics.query;

import org.rsrg.semantics.DuplicateSymbolException;
import org.rsrg.semantics.MathSymbolTable;
import org.rsrg.semantics.Scope;
import org.rsrg.semantics.symbol.Symbol;

import java.util.List;

public interface MultimatchSymbolQuery<E extends Symbol> extends SymbolQuery<E> {

    /**
     * Behaves just as {@link SymbolQuery#searchFromContext}, except that it
     * cannot throw a {@link DuplicateSymbolException}.
     */
    @Override public List<E> searchFromContext(Scope source, MathSymbolTable repo);
}
