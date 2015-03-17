package resolvelite.semantics;

public interface Scope {

    public String getScopeName();

    /**
     * Returns the <code>Scope</code> surrounding this <code>Scope</code>.
     * In practice, this defines where to look next for symbols.
     * 
     * @return The surrounding, 'next' <code>Scope</code>.
     */
    public Scope getEnclosingScope();

    /**
     * Defines a {@link Symbol}, <code>sym</code>, for this <code>Scope</code>.
     */
    public void define(Symbol sym);

    /**
     * Look up <code>name</code> in this scope or in an enclosing
     * <code>Scope</code> if not already present.
     * 
     * @param name The name of the <code>Scope</code> to lookup.
     * @return the found symbol, <code>null</code> if not present.
     */
    public Symbol resolve(String name);
}
