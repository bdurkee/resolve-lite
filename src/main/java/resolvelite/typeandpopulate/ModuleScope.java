package resolvelite.typeandpopulate;

public interface ModuleScope extends Scope {

    /**
     * <p>Returns a <code>ModuleIdentifier</code> that can be used to refer
     * to the module who's scope is represented by this
     * <code>ModuleScope</code>.</p>
     *
     * @return The <code>ModuleIdentifier</code>.
     */
    public ModuleIdentifier getModuleIdentifier();
}
