package org.resolvelite.semantics;

import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.resolvelite.compiler.ErrorKind;
import org.resolvelite.compiler.ResolveCompiler;
import org.resolvelite.semantics.absyn.MExp;
import org.resolvelite.semantics.symbol.MathSymbol;
import org.resolvelite.semantics.symbol.Symbol;
import org.resolvelite.typereasoning.TypeGraph;

import java.util.HashMap;
import java.util.Map;

public class SymbolTable {

    public Map<String, ModuleScope> moduleScopes = new HashMap<>();
    public ParseTreeProperty<Scope> scopes = new ParseTreeProperty<>();
    public ParseTreeProperty<Type> types = new ParseTreeProperty<>();
    public ParseTreeProperty<MExp> mathExps = new ParseTreeProperty<>();

    public static boolean definitionPhaseComplete = false;

    private final ResolveCompiler compiler;
    private final TypeGraph typeGraph;
    private final PredefinedScope globalScope;

    public SymbolTable(ResolveCompiler rc) {
        this.compiler = rc;
        this.typeGraph = new TypeGraph();
        this.globalScope = new PredefinedScope(this);
        initMathTypeSystem(typeGraph);
    }

    public static void seal() {
        definitionPhaseComplete = true;
    }

    private void initMathTypeSystem(TypeGraph g) {
        try {
            globalScope.define(new MathSymbol("Cls", g.CLS, g.CLS, null,
                    globalScope.rootModuleID));
            globalScope.define(new MathSymbol("SSet", g.CLS, g.SSET, null,
                    globalScope.rootModuleID));
            globalScope.define(new MathSymbol("B", g.SSET, g.BOOLEAN, null,
                    globalScope.rootModuleID));
        }
        catch (DuplicateSymbolException dse) {
        }
        //   defineMathSymbol("B", typeGraph.SSET, typeGraph.BOOLEAN);
        //   defineMathSymbol("SSet", typeGraph.CLS, typeGraph.SSET);
        //   defineMathSymbol("Cls", typeGraph.CLS, typeGraph.CLS);
        //   defineMathSymbol("Powerset", typeGraph.POWERSET, null);
        //   defineMathSymbol("Empty_Set", typeGraph.EMPTY_SET, null);
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

    @NotNull public TypeGraph getTypeGraph() {
        return typeGraph;
    }

}
