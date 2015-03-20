package resolvelite.semantics;

import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import resolvelite.parsing.ResolveBaseListener;
import resolvelite.parsing.ResolveParser;
import resolvelite.typereasoning.TypeGraph;

public class ComputeTypes extends SetScopes {

    ParseTreeProperty<MTType> mathTypes = new ParseTreeProperty<>();
    ParseTreeProperty<MTType> mathTypeValues = new ParseTreeProperty<>();
    TypeGraph g;

    public ComputeTypes(SymbolTable symtab,
            @NotNull DefSymbolsAndScopes scopeRepo) {
        super(symtab, scopeRepo);
        this.g = symtab.getTypeGraph();
    }

    @Override
    public void exitMathBooleanExp(
            @NotNull ResolveParser.MathBooleanExpContext ctx) {
        mathTypes.put(ctx, g.BOOLEAN);
    }

    @Override
    public void exitMathPrimaryExp(
            @NotNull ResolveParser.MathPrimaryExpContext ctx) {
        mathTypes.put(ctx, mathTypes.get(ctx.getChild(0)));
    }

    @Override
    public void
            exitMathPrimeExp(@NotNull ResolveParser.MathPrimeExpContext ctx) {
        mathTypes.put(ctx, mathTypes.get(ctx.mathPrimaryExp()));
    }

    @Override
    public void exitMathTypeExp(@NotNull ResolveParser.MathTypeExpContext ctx) {
        mathTypes.put(ctx, mathTypes.get(ctx.mathExp()));
    }

    @Override
    public void exitMathVariableExp(
            @NotNull ResolveParser.MathVariableExpContext ctx) {
        MathSymbol e = currentScope.resolve(ctx.name.getText()).toMathSymbol();
        mathTypes.put(ctx, e.getMathType());
    }

}
