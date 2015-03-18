package resolvelite.semantics;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.misc.Nullable;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import resolvelite.compiler.ResolveCompiler;
import resolvelite.semantics.symbol.MathSymbol;
import resolvelite.typereasoning.TypeGraph;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class SymbolTable extends ScopeRepository {

    private final Map<ModuleIdentifier, ModuleScope> moduleScopes =
            new HashMap<ModuleIdentifier, ModuleScope>();

    private final Deque<Scope> lexicalScopeStack = new LinkedList<Scope>();

    private final ParseTreeProperty<Scope> scopes =
            new ParseTreeProperty<Scope>();

    @NotNull private final ResolveCompiler compiler;


    @NotNull private final TypeGraph typeGraph;
    @Nullable private ModuleScope currentModuleScope;

    public SymbolTable(@NotNull ResolveCompiler rc) {
        this.typeGraph = new TypeGraph(rc);
        this.compiler = rc;

        Scope topLevelScope = new LocalScope(null);
        initBuiltinMathTypes(typeGraph, topLevelScope);

        //install the top level scope
        lexicalScopeStack.push(topLevelScope);
    }

    protected void initBuiltinMathTypes(TypeGraph g, Scope s) {
        s.define(new MathSymbol(g, "B", null, null, ModuleIdentifier.GLOBAL));
    }

    public ModuleScope startModuleScope(@NotNull ParserRuleContext ctx) {
        Scope topLvlScope = lexicalScopeStack.peek();
        ModuleScope s = new ModuleScope(typeGraph, ctx, topLvlScope, this);
        this.currentModuleScope = s;
        addScope(s, parent);
        myModuleScopes.put(s.getModuleIdentifier(), s);
        return s;
    }

    public Scope endScope() {

    }

    @Override
    public ModuleScope getModuleScope(ModuleIdentifier module) {
        return moduleScopes.get(module);
    }

    @Override
    public TypeGraph getTypeGraph() {
        return typeGraph;
    }

    @Override
    public ResolveCompiler getCompiler() {
        return compiler;
    }
}
