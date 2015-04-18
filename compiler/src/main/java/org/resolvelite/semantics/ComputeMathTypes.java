package org.resolvelite.semantics;

import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.resolvelite.compiler.ResolveCompiler;
import org.resolvelite.parsing.ResolveBaseListener;
import org.resolvelite.parsing.ResolveParser;
import org.resolvelite.semantics.symbol.MathType;

class ComputeMathTypes extends SetScopes {

    ParseTreeProperty<MathType> mathTypes;

    public ComputeMathTypes(@NotNull ResolveCompiler compiler,
                        @NotNull SymbolTable symtab) {
        super(compiler, symtab);
        this.mathTypes = symtab.mathTypes;
    }

    @Override public void exitMathBooleanExp(
            @NotNull ResolveParser.MathBooleanExpContext ctx) {

    }
}
