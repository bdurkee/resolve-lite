package resolvelite.semantics;

public class ModuleScope extends BaseScope  {

    public ModuleScope(Scope enclosingScope) {
        super(enclosingScope);
    }

    public String getScopeName() {
        return "module scope";
    }
}
