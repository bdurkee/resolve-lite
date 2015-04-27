package org.resolvelite.misc;

import org.resolvelite.compiler.ErrorKind;
import org.resolvelite.compiler.ResolveCompiler;
import org.resolvelite.semantics.DuplicateSymbolException;
import org.resolvelite.semantics.ScopeBuilder;
import org.resolvelite.typereasoning.TypeGraph;

public class Hardcoded {

    public static void addBuiltInSymbols(TypeGraph g, ResolveCompiler rc,
            ScopeBuilder b) {
        try {
            b.addBinding("Cls", null, g.CLS, g.CLS);
            b.addBinding("SSet", null, g.CLS, g.SSET);
            b.addBinding("Entity", null, g.CLS, g.ENTITY);
            b.addBinding("B", null, g.SSET, g.BOOLEAN);
            b.addBinding("Z", null, g.SSET, g.Z);
            b.addBinding("true", null, g.BOOLEAN);
            b.addBinding("false", null, g.BOOLEAN);
            b.addBinding("Powerset", null, g.POWERSET);
        }
        catch (DuplicateSymbolException e) {
            rc.errorManager.semanticError(ErrorKind.DUP_SYMBOL, null,
                    e.getExistingSymbol());
        }
    }
}
