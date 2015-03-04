package resolvelite.typeandpopulate;

import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import resolvelite.compiler.ResolveCompiler;
import resolvelite.misc.HardCoded;
import resolvelite.typereasoning.TypeGraph;

import java.util.Deque;
import java.util.LinkedList;

public class MathSymbolTableBuilder {

    private static final Scope DUMMY_RESOLVER = new DummyIdentifierResolver();

    /**
     * <p>A mapping from {@link ParseTree}s to the scopes they introduce.</p>
     */
    private final ParseTreeProperty<ScopeBuilder> scopes =
            new ParseTreeProperty<ScopeBuilder>();

    private final Deque<ScopeBuilder> lexicalScopeStack =
            new LinkedList<ScopeBuilder>();

    @NotNull private final TypeGraph typeGraph;
    @NotNull private final ResolveCompiler compiler;

    public MathSymbolTableBuilder(ResolveCompiler rc) {
        this.typeGraph = new TypeGraph();
        this.compiler = rc;
        ScopeBuilder globalScope =
                new ScopeBuilder(this, typeGraph, null, DUMMY_RESOLVER,
                        ModuleIdentifier.GLOBAL);

        HardCoded.initMathTypeSystem(typeGraph, globalScope);
    }

    @NotNull
    public ResolveCompiler getCompiler() {
        return compiler;
    }

    @NotNull
    public TypeGraph getTypeGraph() {
        return typeGraph;
    }

    public ScopeBuilder startModuleScope(@NotNull ParseTree t) {

        //since we're dealing currently only with one monolithic scope,
        //we omit this check for now.
        //checkModuleScopeOpen();
        lexicalScopeStack.push()
        ScopeBuilder parent = lexicalScopeStack.peek();

        ScopeBuilder s = new ScopeBuilder(this, typeGraph, t, parent);
        addScope(s, parent);
        return s;
    }

    public ScopeBuilder endScope() {
        if (lexicalScopeStack.size() == 1) {
            throw new IllegalStateException("No open scope.");
        }
        lexicalScopeStack.pop();
        ScopeBuilder result;

        if (lexicalScopeStack.size() == 1) {
            result = null;
        }
        else {
            result = lexicalScopeStack.peek();
        }
        return result;
    }

    private void addScope(ScopeBuilder s, ScopeBuilder parent) {
        parent.addChild(s);
        lexicalScopeStack.push(s);
        scopes.put(s.getDefiningElement(), s);
    }

}
