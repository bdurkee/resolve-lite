package resolvelite.typeandpopulate;

import resolvelite.typeandpopulate.entry.SymbolTableEntry;
import resolvelite.typeandpopulate.query.MultimatchSymbolQuery;

import java.util.List;

/**
 * <p>A <code>Scope</code> represents a mapping from symbol names to symbol
 * table entries.  Each entry inherits from
 * {@link SymbolTableEntry SymbolTableEntry}, but differing concrete subclasses
 * represent different kinds of entries.</p>
 *
 * <p>This interface defines no mutator methods, but specific concrete
 * subclasses may be mutable.</p>
 *
 * <p>A scope may possibly exist in a context in which more symbols are
 * available than just those introduced directly inside the scope.  For example,
 * a scope may be a child scope of another, or may exist within a RESOLVE module
 * that imports other modules (and, thus, their contained symbols).  The methods
 * of <code>Scope</code> provide options for searching these possible additional
 * available scopes in different ways.</p>
 */
public interface Scope {

    /**
     * <p>Searches for symbols by the given query, using this <code>Scope</code>
     * as the source scope of the search, i.e. the scope that is the context
     * from which the search was triggered.</p>
     *
     * @param query The query to use.
     *
     * @return A list of all symbols matching the given query.
     */
    public <E extends SymbolTableEntry> List<E> query(
            MultimatchSymbolQuery<E> query);
}
