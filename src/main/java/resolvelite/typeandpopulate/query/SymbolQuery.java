package resolvelite.typeandpopulate.query;

import resolvelite.typeandpopulate.DuplicateSymbolException;
import resolvelite.typeandpopulate.Scope;
import resolvelite.typeandpopulate.ScopeRepository;
import resolvelite.typeandpopulate.entry.SymbolTableEntry;

import java.util.List;

public interface SymbolQuery<E extends SymbolTableEntry> {

    /**
     * <p>Given a source {@link Scope} and a {@link ScopeRepository} containing
     * any imports, from which <code>source</code> is drawn, searches them
     * appropriately, returning a list of matching {@link SymbolTableEntry}s
     * that are subtypes of <code>E</code>.</p>
     *
     * <p>If there are no matches, returns an empty list.  If more than one
     * match is found where no more than one was expected, throws a
     * {@link DuplicateSymbolException DuplicateSymbolException}.</p>
     *
     * @param source The source scope from which the search was spawned.
     * @param repo A repository of any referenced modules.
     *
     * @return A list of matches.
     */
    public List<E> searchFromContext(Scope source, ScopeRepository repo)
            throws DuplicateSymbolException;
}