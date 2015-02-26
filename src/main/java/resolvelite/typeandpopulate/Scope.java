package resolvelite.typeandpopulate;

import resolvelite.typeandpopulate.entry.SymbolTableEntry;

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

    public String getScopeName();

    /** Where to look next for symbols */
    public Scope getEnclosingScope();

    /** Look up name in this scope or in enclosing scope if not here */
    public SymbolTableEntry resolve(String name);
}
