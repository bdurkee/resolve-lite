package org.resolvelite.misc;

import org.resolvelite.compiler.ErrorKind;
import org.resolvelite.compiler.ResolveCompiler;
import org.resolvelite.semantics.DuplicateSymbolException;
import org.resolvelite.semantics.MTFunction.MTFunctionBuilder;
import org.resolvelite.semantics.ScopeBuilder;
import org.resolvelite.typereasoning.TypeGraph;

public class Hardcoded {

    public static void addBuiltInSymbols(TypeGraph g, ResolveCompiler rc,
            ScopeBuilder b) {
        try {
            b.addBinding("Cls", null, g.CLS, g.CLS);
            b.addBinding("SSet", null, g.CLS, g.SSET);
            b.addBinding("Empty_Set", null, g.SSET, g.EMPTY_SET);
            b.addBinding("Card", null, g.CLS, g.CARD);

            b.addBinding("Entity", null, g.CLS, g.ENTITY);
            b.addBinding("B", null, g.SSET, g.BOOLEAN);
            b.addBinding("Z", null, g.SSET, g.Z);
            b.addBinding("N", null, g.SSET, g.Z);
            b.addBinding("true", null, g.BOOLEAN);
            b.addBinding("false", null, g.BOOLEAN);
            b.addBinding("Powerset", null, g.POWERSET);
            b.addBinding("union", null, g.UNION);
            b.addBinding("min_int", null, g.Z, null);
            b.addBinding("max_int", null, g.Z, null);

            b.addBinding("+", null,
                    new MTFunctionBuilder(g, g.Z).paramTypes(g.Z, g.Z).build());
            b.addBinding("-", null,
                    new MTFunctionBuilder(g, g.Z).paramTypes(g.Z, g.Z).build());
            b.addBinding("<", null, new MTFunctionBuilder(g, g.BOOLEAN)
                    .paramTypes(g.Z, g.Z).build());
            b.addBinding("<=", null, new MTFunctionBuilder(g, g.BOOLEAN)
                    .paramTypes(g.Z, g.Z).build());
            b.addBinding(">", null, new MTFunctionBuilder(g, g.BOOLEAN)
                    .paramTypes(g.Z, g.Z).build());
            b.addBinding(">=", null, new MTFunctionBuilder(g, g.BOOLEAN)
                    .paramTypes(g.Z, g.Z).build());
            b.addBinding("=", null, new MTFunctionBuilder(g, g.BOOLEAN)
                    .paramTypes(g.ENTITY, g.ENTITY).build());
            b.addBinding("and", null, new MTFunctionBuilder(g, g.BOOLEAN)
                    .paramTypes(g.BOOLEAN, g.BOOLEAN).build());
            b.addBinding("||...||", null, new MTFunctionBuilder(g, g.CARD)
                    .paramTypes(g.SSET).build());
        }
        catch (DuplicateSymbolException e) {
            rc.errorManager.semanticError(ErrorKind.DUP_SYMBOL, null,
                    e.getExistingSymbol());
        }
    }
}
