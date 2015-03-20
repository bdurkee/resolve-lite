package resolvelite.semantics;

import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.misc.Nullable;
import org.antlr.v4.runtime.tree.ParseTree;
import resolvelite.compiler.ResolveCompiler;
import resolvelite.typereasoning.TypeGraph;

import java.util.List;
import java.util.Set;

public interface Scope {

    @NotNull
    public String getScopeDescription();

    @Nullable
    public Scope getParentScope();

    /**
     * Returns the <code>Scope</code> surrounding this <code>Scope</code>.
     * In practice, this defines where to look next for symbols.
     * 
     * @return The surrounding, 'next' <code>Scope</code>.
     */
    @Nullable
    public Scope getEnclosingScope();

    /**
     * Returns all scopes that are immediate decendents of <code>this</code>
     * <code>Scope</code>.
     * 
     * @return A list of nested <code>Scope</code>s of <code>this</code>.
     */
    public List<Scope> getNestedScopes();

    /**
     * Set which <code>Scope</code> encloses this scope. E.g., if this scope is
     * a function, the enclosing scope could be a class. The {@link BaseScope}
     * class automatically adds this to nested scope list of s.
     * 
     * @param s The enclosing scope.
     */
    public void setEnclosingScope(Scope s);

    public List<? extends Symbol> getSymbols();

    public Set<String> getSymbolNames();

    public int getNumberOfSymbols();

    /**
     * Define an {@link Symbol} in the current scope, throw
     * {@link IllegalArgumentException} if {@link Symbol} already defined in
     * this scope. Set insertion order number of {@link Symbol}.
     * 
     * @throws IllegalArgumentException if <code>sym</code> has already been
     *         defined.
     * @param sym The symbol we're adding.
     */
    public void define(@NotNull Symbol sym) throws IllegalArgumentException;

    /**
     * Look up <code>name</code> in this scope or in an enclosing
     * <code>Scope</code> if not already present.
     * 
     * @param name The name of the <code>Scope</code> to lookup.
     * @return the found symbol, <code>null</code> if not present.
     */
    @Nullable
    public Symbol resolve(String name);

    public Symbol getSymbol(String name);

}
