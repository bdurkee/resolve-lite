package org.resolvelite.semantics;

import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.resolvelite.compiler.ErrorKind;
import org.resolvelite.compiler.ResolveCompiler;
import org.resolvelite.semantics.symbol.MathSymbol;
import org.resolvelite.typereasoning.TypeGraph;

import java.util.HashMap;
import java.util.Map;

public class SymbolTable {

    public Map<String, ModuleScope> moduleScopes = new HashMap<>();
    public ParseTreeProperty<Scope> scopes = new ParseTreeProperty<>();
    public ParseTreeProperty<Type> types = new ParseTreeProperty<>();

    public static boolean definitionPhaseComplete = false;

    private final ResolveCompiler compiler;
    private final PredefinedScope globalScope;
    private final TypeGraph g;

    public SymbolTable(ResolveCompiler rc) {
        this.compiler = rc;
        this.globalScope = new PredefinedScope(this);
        this.g = new TypeGraph();
        initMathTypeSystem(g, globalScope);
    }

    public TypeGraph getTypeGraph() {
        return g;
    }

    public static void seal() {
        definitionPhaseComplete = true;
    }

    private static void initMathTypeSystem(TypeGraph g, PredefinedScope s) {
        try {
            s.define(new MathSymbol("Cls", g.CLS, s.getScopeDescription()));
            s.define(new MathSymbol("SSet", g.SSET, s.getScopeDescription()));
            s.define(new MathSymbol("B", g.BOOLEAN, s.getScopeDescription()));
        } catch (DuplicateSymbolException e) {
            System.out.println("Duplicate symbol dse");
        }
    }

    public ModuleScope getModuleScope(String name) throws NoSuchSymbolException {
        ModuleScope module = moduleScopes.get(name);
        if ( module == null ) {
            compiler.errorManager.semanticError(ErrorKind.NO_SUCH_MODULE, null,
                    name);
            throw new NoSuchSymbolException();
        }
        return module;
    }

    @NotNull public PredefinedScope getGlobalScope() {
        return globalScope;
    }

    @NotNull public ResolveCompiler getCompiler() {
        return compiler;
    }

}
