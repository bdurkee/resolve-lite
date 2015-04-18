package org.resolvelite.semantics;

import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.resolvelite.compiler.ErrorKind;
import org.resolvelite.compiler.ResolveCompiler;
import org.resolvelite.parsing.ResolveParser;
import org.resolvelite.semantics.symbol.MathSymbol;

class ComputeMathTypes extends SetScopes {

    private final MathTypeInvalid invalid;
    ParseTreeProperty<MathType> mathTypes = new ParseTreeProperty<>();

    ComputeMathTypes(@NotNull ResolveCompiler rc, @NotNull SymbolTable symtab) {
        super(rc, symtab);
        this.invalid = new MathTypeInvalid(symtab.getTypeGraph());
    }

    @Override public void exitMathBooleanExp(
            @NotNull ResolveParser.MathBooleanExpContext ctx) {
        try {
            MathSymbol b = (MathSymbol)currentScope.resolve(null,
                    ctx.getText(), false);
            mathTypes.put(ctx, b.getMathType());
        }
        catch (NoSuchSymbolException e) {
            symtab.getCompiler().errorManager.semanticError(
                    ErrorKind.NO_SUCH_SYMBOL, ctx.getStart(), ctx.getText());
            mathTypes.put(ctx, invalid);
        }
    }

    @Override public void exitMathPrimaryExp(
            @NotNull ResolveParser.MathPrimaryExpContext ctx) {
        mathTypes.put(ctx, mathTypes.get(ctx.getChild(0)));
    }

    @Override public void exitMathPrimeExp(
            @NotNull ResolveParser.MathPrimeExpContext ctx) {
        mathTypes.put(ctx, mathTypes.get(ctx.mathPrimaryExp()));
    }

    @Override public void exitMathAssertionExp(
            @NotNull ResolveParser.MathAssertionExpContext ctx) {
        mathTypes.put(ctx, mathTypes.get(ctx.mathExp()));
    }

}
