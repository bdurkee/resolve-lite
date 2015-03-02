package resolvelite.typeandpopulate;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import resolvelite.ResolveCompiler;
import resolvelite.parsing.ResolveParser;
import resolvelite.typereasoning.TypeGraph;

public class MathSymbolTableBuilder {

    /**
     * <p>A mapping from {@link ParseTree}s to the scopes they introduce.</p>
     */
    private final ParseTreeProperty<ScopeBuilder> scopes =
            new ParseTreeProperty<ScopeBuilder>();

    @NotNull private final TypeGraph typeGraph;
    @NotNull private final ResolveCompiler compiler;

    public MathSymbolTableBuilder(ResolveCompiler rc) {
        this.typeGraph = new TypeGraph();
        this.compiler = rc;
    }

    @NotNull
    public ResolveCompiler getCompiler() {
        return compiler;
    }

    @NotNull
    public TypeGraph getTypeGraph() {
        return typeGraph;
    }

    public void startScope(@NotNull ParseTree t) {

    }

}
