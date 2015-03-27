package resolvelite.semantics;

import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.misc.Nullable;
import org.antlr.v4.runtime.tree.ParseTree;
import resolvelite.typereasoning.TypeGraph;

public class LocalScope extends BaseScope {

    public LocalScope(Scope enclosingScope, SymbolTable scopeRepo) {
        super(enclosingScope, scopeRepo);
    }

    @Override public String getScopeDescription() {
        return "local";
    }
}
