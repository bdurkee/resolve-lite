package resolvelite.semantics;

import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.misc.Nullable;
import org.antlr.v4.runtime.tree.ParseTree;
import resolvelite.typereasoning.TypeGraph;

public class ModuleScope extends BaseScope {

    public ModuleScope(Scope scope) {
        super(scope);
    }

    @Override
    public String getScopeDescription() {
        return "module";
    }
}
