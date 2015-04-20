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
            b.addBinding("Cls", g.CLS, g.CLS);
            b.addBinding("SSet", g.CLS, g.SSET);
            b.addBinding("Entity", g.CLS, g.ENTITY);
            b.addBinding("B", g.SSET, g.BOOLEAN);
        }
        catch (DuplicateSymbolException e) {
            rc.errorManager.semanticError(ErrorKind.DUP_SYMBOL, null, e
                    .getExistingEntry().getName());
        }
    }
}
