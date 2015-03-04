package resolvelite.typeandpopulate.query;

import resolvelite.typeandpopulate.Scope;
import resolvelite.typeandpopulate.ScopeRepository;
import resolvelite.typeandpopulate.entry.SymbolTableEntry;

import java.util.List;

/**
 * <p>Refines {@link SymbolQuery} by guaranteeing that
 * {@link #searchFromContext(Scope, ScopeRepository)} will not throw a
 * {@link DuplicateSymbolException}.</p>
 */
public interface MultimatchSymbolQuery<E extends SymbolTableEntry>
        extends
            SymbolQuery<E> {

    /**
     * <p>Behaves just as
     * {@link SymbolQuery#searchFromContext(Scope, ScopeRepository)
     * SymbolQuery.searchFromContext()}, except that it cannot throw a
     * {@link DuplicateSymbolException}.</p>
     */
    @Override
    public List<E> searchFromContext(Scope source, ScopeRepository repo);
}
