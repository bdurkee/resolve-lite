package resolvelite.typeandpopulate;

import java.util.List;

public interface ModuleScope extends Scope {

    /**
     * <p>Returns a <code>ModuleIdentifier</code> that can be used to refer
     * to the module who's scope is represented by this
     * <code>ModuleScope</code>.</p>
     *
     * @return The <code>ModuleIdentifier</code>.
     */
    public ModuleIdentifier getModuleIdentifier();

    /**
     * <p>Returns <code>true</code> <strong>iff</code> the module who's scope
     * is represented by this <code>ModuleScope</code> imports the given
     * module.  Note that, by definition, all modules import themselves.</p>
     */
    public boolean imports(ModuleIdentifier i);

    /**
     * <p>Returns a list of modules that the module who's scop is represented by
     * this <code>ModuleScope</code> imports, not including itself (which all
     * modules are defined to import).  This list is a copy and modifying it
     * will not impact the behavior of this <code>ModuleScope</code>.</p>
     *
     * @return A list of imported modules.
     */
    public List<ModuleIdentifier> getImports();
}
