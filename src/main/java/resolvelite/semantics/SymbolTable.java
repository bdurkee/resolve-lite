package resolvelite.semantics;

import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import resolvelite.compiler.ResolveCompiler;
import resolvelite.misc.Utils;
import resolvelite.semantics.symbol.ProgTypeDefinitionSymbol;
import resolvelite.semantics.symbol.Symbol;
import resolvelite.typereasoning.TypeGraph;

import java.util.HashMap;
import java.util.Map;

public class SymbolTable {

    public static final ProgTypeDefinitionSymbol VOID =
            new ProgTypeDefinitionSymbol("Void");
    public Map<String, ModuleScope> moduleScopes = new HashMap<>();
    public ParseTreeProperty<Scope> scopes = new ParseTreeProperty<>();

    private final ResolveCompiler compiler;
    private final TypeGraph typeGraph;

    public SymbolTable(ResolveCompiler rc) {
        this.compiler = rc;
        this.typeGraph = new TypeGraph();
        initMathTypeSystem();
        initProgramTypeSystem();
    }

    private void initProgramTypeSystem() {
        try {
            definePredefinedSymbol(new ProgTypeDefinitionSymbol("Boolean"));
            definePredefinedSymbol(new ProgTypeDefinitionSymbol("Integer"));
        }
        catch (DuplicateSymbolException dse) {
            throw new RuntimeException("Dup sym. Todo: put the actual Symbol "
                    + "as a field inside the dse exception.");
        }
    }

    private void initMathTypeSystem() {
        //   defineMathSymbol("B", typeGraph.SSET, typeGraph.BOOLEAN);
        //   defineMathSymbol("SSet", typeGraph.CLS, typeGraph.SSET);
        //   defineMathSymbol("Cls", typeGraph.CLS, typeGraph.CLS);
        //   defineMathSymbol("Powerset", typeGraph.POWERSET, null);
        //   defineMathSymbol("Empty_Set", typeGraph.EMPTY_SET, null);
    }

    // public void
    //         defineMathSymbol(String name, MathType type, MathType typeValue) {
    //    MathSymbol result = new MathSymbol(typeGraph, name, type, typeValue);
    //    definePredefinedSymbol(result);
    //  }

    protected void definePredefinedSymbol(Symbol s)
            throws DuplicateSymbolException {
        PredefinedScope.INSTANCE.define(s);
    }

    @NotNull
    public ResolveCompiler getCompiler() {
        return compiler;
    }

    @NotNull
    public TypeGraph getTypeGraph() {
        return typeGraph;
    }

}
