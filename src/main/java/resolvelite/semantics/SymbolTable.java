package resolvelite.semantics;

import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import resolvelite.compiler.ResolveCompiler;
import resolvelite.misc.Utils;
import resolvelite.typereasoning.TypeGraph;

import java.util.HashMap;
import java.util.Map;

public class SymbolTable {

    Map<String, ModuleScope> moduleScopes = new HashMap<>();
    ParseTreeProperty<Scope> scopes = new ParseTreeProperty<>();

    private final ResolveCompiler compiler;
    private final TypeGraph typeGraph;

    public SymbolTable(ResolveCompiler rc) {
        this.compiler = rc;
        this.typeGraph = new TypeGraph(rc);
        initMathTypeSystem();
    }

    private void initMathTypeSystem() {
        defineMathSymbol("B", typeGraph.SSET, typeGraph.BOOLEAN);
        defineMathSymbol("SSet", typeGraph.CLS, typeGraph.SSET);
    }

    public void
            defineMathSymbol(String name, MathType type, MathType typeValue) {
        MathSymbol result = new MathSymbol(typeGraph, name, type, typeValue);
        definePredefinedSymbol(result);
    }

    public void definePredefinedSymbol(Symbol s) {
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

    public static String toString(Scope s) {
        StringBuilder buf = new StringBuilder();
        toString(buf, s, 0);
        return buf.toString();
    }

    public static void toString(StringBuilder buf, Scope s, int level) {
        buf.append(Utils.tab(level));
        buf.append(s.getScopeDescription());
        buf.append("\n");
        level++;
        for (Symbol sym : s.getSymbols()) {
            if ( !(sym instanceof Scope) ) {
                buf.append(Utils.tab(level));
                buf.append(sym + "\n");
            }
        }
        for (Scope nested : s.getNestedScopes()) {
            toString(buf, nested, level);
        }
        level--;
    }
}
