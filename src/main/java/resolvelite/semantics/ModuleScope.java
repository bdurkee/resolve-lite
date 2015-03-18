package resolvelite.semantics;

import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.misc.Nullable;
import org.antlr.v4.runtime.tree.ParseTree;
import resolvelite.typereasoning.TypeGraph;

public class ModuleScope extends BaseScope {

    @NotNull private final SymbolTable symbolTable;

    public ModuleScope(TypeGraph g, @Nullable ParseTree definingTree,
            @Nullable Scope parent, @NotNull SymbolTable t) {
        super(t, definingTree, parent, new ModuleIdentifier(definingTree));
        this.symbolTable = t;
    }

    public String getScopeName() { return "local scope"; }
}
