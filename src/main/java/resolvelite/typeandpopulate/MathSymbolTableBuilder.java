package resolvelite.typeandpopulate;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import resolvelite.parsing.ResolveParser;
import resolvelite.typereasoning.TypeGraph;

public class MathSymbolTableBuilder {

    /**
     * <p>A mapping from {@link ParseTree}s to the scopes they introduce.</p>
     */
    private final ParseTreeProperty<ScopeBuilder> scopes =
            new ParseTreeProperty<ScopeBuilder>();

    private final TypeGraph myTypeGraph;

    public MathSymbolTableBuilder() {
        myTypeGraph = new TypeGraph();
    }

    public TypeGraph getTypeGraph() {
        return myTypeGraph;
    }

    public void startScope(@NotNull ParseTree t) {

    }

}
