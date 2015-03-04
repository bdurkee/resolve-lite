package resolvelite.typeandpopulate;

import org.antlr.v4.runtime.tree.ParseTree;
import resolvelite.typereasoning.TypeGraph;
import sun.jvm.hotspot.debugger.NoSuchSymbolException;

public abstract class ScopeRepository {

    /**
     * <p>Returns the {@link ModuleScope} associated with the given
     * {@link ModuleIdentifier}.</p>
     *
     * @param module The module identifier.
     * @return The associated module scope.
     *
     * @throws NoSuchSymbolException If no scope has been opened for the named
     *             module.
     */
    public abstract ModuleScope getModuleScope(ModuleIdentifier module)
            throws NoSuchSymbolException;

    /**
     * <p>Returns the {@link Scope} introduced and bounded by the given
     * defining element.</p>
     *
     * @param e defining element.
     * @return The associated scope.
     *
     * @throws NoSuchSymbolException If no scope has been opened for the given
     *             defining element.
     */
    public abstract Scope getScope(ParseTree e);

    /**
     * <p>Returns the {@link TypeGraph} that relates the types found in this
     * <code>ScopeRepository</code>.</p>
     *
     * @return The <code>TypeGraph</code>.
     */
    public abstract TypeGraph getTypeGraph();
}
