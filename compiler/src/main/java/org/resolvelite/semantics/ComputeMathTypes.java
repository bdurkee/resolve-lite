package org.resolvelite.semantics;

import org.antlr.v4.runtime.misc.NotNull;
import org.resolvelite.parsing.ResolveBaseListener;
import org.resolvelite.parsing.ResolveParser;
import org.resolvelite.semantics.absyn.MExp;
import org.resolvelite.semantics.absyn.MSymbolExp;
import org.resolvelite.typereasoning.TypeGraph;

/**
 * Builds a math-typed, {@link MExp} for each mathematical expression appearing
 * in the parse tree representing the current module. References to built
 * {@link MExp}s can be accessed through {@link SymbolTable}.
 */
public class ComputeMathTypes extends ResolveBaseListener {

    private final SymbolTable symtab;
    private final TypeGraph g;

    public ComputeMathTypes(@NotNull SymbolTable symtab) {
        this.symtab = symtab;
        this.g = symtab.getTypeGraph();
    }

    @Override public void exitMathDefinitionDecl(
            @NotNull ResolveParser.MathDefinitionDeclContext ctx) {

    }

    @Override public void exitMathTypeExp(
            @NotNull ResolveParser.MathTypeExpContext ctx) {
        symtab.mathExps.put(ctx, symtab.mathExps.get(ctx.mathExp()));
    }

    @Override public void exitMathAssertionExp(
            @NotNull ResolveParser.MathAssertionExpContext ctx) {
        symtab.mathExps.put(ctx, symtab.mathExps.get(ctx.mathExp()));
    }

    @Override public void exitMathPrimeExp(
            @NotNull ResolveParser.MathPrimeExpContext ctx) {
        symtab.mathExps.put(ctx, symtab.mathExps.get(ctx.mathPrimaryExp()));
    }

    @Override public void exitMathPrimaryExp(
            @NotNull ResolveParser.MathPrimaryExpContext ctx) {
        symtab.mathExps.put(ctx, symtab.mathExps.get(ctx.getChild(0)));
    }

    @Override public void exitMathBooleanExp(
            @NotNull ResolveParser.MathBooleanExpContext ctx) {
        MSymbolExp result =
                new MSymbolExp.MSymbolExpBuilder(ctx.BooleanLiteral().getText())
                        .literal(true) //
                        .mathType(g.SSET) //
                        .mathTypeValue(g.BOOLEAN) //
                        .build();
        symtab.mathExps.put(ctx, result);
    }
}
