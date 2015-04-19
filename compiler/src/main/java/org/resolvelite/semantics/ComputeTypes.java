package org.resolvelite.semantics;

import org.antlr.v4.runtime.misc.NotNull;
import org.resolvelite.compiler.ResolveCompiler;

public class ComputeTypes extends SetScopes {

    public ComputeTypes(@NotNull ResolveCompiler rc, @NotNull SymbolTable symtab) {
        super(rc, symtab);
    }
}
