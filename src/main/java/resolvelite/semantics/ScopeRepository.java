package resolvelite.semantics;

import resolvelite.compiler.ResolveCompiler;
import resolvelite.compiler.tree.ResolveAnnotatedParseTree.TreeAnnotatingBuilder;
import resolvelite.typereasoning.TypeGraph;

/**
 * A <code>SymbolTable</code> maps {@link TreeAnnotatingBuilder}s and
 * {@link ModuleIdentifier}s to the {@link Scope}s they introduce.
 *
 * <p>Each <code>SymbolTable</code> has a {@link TypeGraph} that relates
 * the types found in the symbol table.
 *
 * <p>While this base class defines no methods for mutating the symbol table,
 * concrete subclasses may provide mutation methods.  It is particularly
 * important that clients be aware the symbol table may be "under construction"
 * even as they use it.  We therefore favor vocabulary such as "open" and
 * "closed" for scopes rather than "exists", which might imply (erroneously)
 * that scopes spring into existence atomically and fully formed.</p>
 */
public abstract class ScopeRepository {

    /**
     * <p>Returns the {@link ModuleScope} associated with the given
     * {@link ModuleIdentifier}.</p>
     *
     * @param module The module identifier.
     *
     * @returns The associated module scope.
     */
    public abstract ModuleScope getModuleScope(ModuleIdentifier module);

    /**
     * <p>Returns the {@link TypeGraph} that relates the types found in this
     * <code>ScopeRepository</code>.</p>
     *
     * @return The <code>TypeGraph</code>.
     */
    public abstract TypeGraph getTypeGraph();

    public abstract ResolveCompiler getCompiler();
}
