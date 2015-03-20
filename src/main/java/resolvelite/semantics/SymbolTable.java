package resolvelite.semantics;

import resolvelite.compiler.ResolveCompiler;
import resolvelite.misc.Utils;
import resolvelite.semantics.programtypes.PTType;
import resolvelite.typereasoning.TypeGraph;

public class SymbolTable {
    //public static final MTType INVALID_MTTYPE = new InvalidType();
    //public static final PTType INVALID_PTTYPE = new InvalidType();

    public ModuleScope MODULE = new ModuleScope(PredefinedScope.INSTANCE);
    private final ResolveCompiler compiler;
    private final TypeGraph typeGraph;

    public SymbolTable(ResolveCompiler rc) {
        this.compiler = rc;
        this.typeGraph = new TypeGraph(rc);
        initTypeSystem();
    }

    private void initTypeSystem() {
        definePredefinedSymbol(new MathSymbol("B", typeGraph.SSET, typeGraph.BOOLEAN));
        definePredefinedSymbol(new MathSymbol("SSet", typeGraph.CLS, typeGraph.SSET));
    }

    public void definePredefinedSymbol(Symbol s) {
        PredefinedScope.INSTANCE.define(s);
    }

    public void defineModuleSymbol(Symbol s) {
        MODULE.define(s);
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
                buf.append(sym+"\n");
            }
        }
        for (Scope nested : s.getNestedScopes()) {
            toString(buf, nested, level);
        }
        level--;
    }
}
