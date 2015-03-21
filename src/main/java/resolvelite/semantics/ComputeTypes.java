package resolvelite.semantics;

import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import resolvelite.compiler.ErrorKind;
import resolvelite.parsing.ResolveBaseListener;
import resolvelite.parsing.ResolveParser;
import resolvelite.typereasoning.TypeGraph;
import resolvelite.semantics.BaseSymbol.Quantification;

/**
 * Annotates expressions and sub-expressions with appropriate {@link PTType}s
 * and {@link MathType}s.
 */
public class ComputeTypes extends ResolveBaseListener {

    ParseTreeProperty<MathType> mathTypes = new ParseTreeProperty<>();
    ParseTreeProperty<MathType> mathTypeValues = new ParseTreeProperty<>();
    TypeGraph g;
    SymbolTable symtab;
    Scope currentScope;

    public ComputeTypes(SymbolTable symtab, Scope currentScope) {
        this.symtab = symtab;
        this.g = symtab.getTypeGraph();
        this.currentScope = currentScope;
    }

    @Override
    public void exitMathBooleanExp(
            @NotNull ResolveParser.MathBooleanExpContext ctx) {
        mathTypes.put(ctx, g.BOOLEAN);
    }

    @Override
    public void exitMathPrimaryExp(
            @NotNull ResolveParser.MathPrimaryExpContext ctx) {
        ParseTree child = ctx.getChild(0);
        mathTypes.put(ctx, mathTypes.get(child));
        mathTypeValues.put(ctx, mathTypeValues.get(child));
    }

    @Override
    public void
            exitMathPrimeExp(@NotNull ResolveParser.MathPrimeExpContext ctx) {
        mathTypes.put(ctx, mathTypes.get(ctx.mathPrimaryExp()));
        mathTypeValues.put(ctx, mathTypeValues.get(ctx.mathPrimaryExp()));
    }

    @Override
    public void exitMathTypeExp(@NotNull ResolveParser.MathTypeExpContext ctx) {
        mathTypes.put(ctx, mathTypes.get(ctx.mathExp()));
        mathTypeValues.put(ctx, mathTypeValues.get(ctx.mathExp()));
    }

    @Override
    public void exitMathVariableExp(
            @NotNull ResolveParser.MathVariableExpContext ctx) {
        try {
            MathSymbol intendedEntry =
                    (MathSymbol) currentScope.resolve(ctx.name.getText());

            mathTypes.put(ctx, intendedEntry.getMathType());
            if ( intendedEntry.getQuantification() == Quantification.NONE ) {
                mathTypeValues.put(ctx, intendedEntry.getMathTypeValue());
            }
            else {
                throw new UnsupportedOperationException("todo");
                //     if (intendedEntry.getType().isKnownToContainOnlyThingsThatAreTypes()) {
                //         node.setMathTypeValue(new MTNamed(myTypeGraph, symbolName));
                //     }
            }
            String typeValueDesc = "";
            if ( mathTypeValues.get(ctx) != null ) {
                typeValueDesc =
                        ", referencing math type " + mathTypeValues.get(ctx)
                                + " (" + mathTypeValues.get(ctx).getClass()
                                + ")";
            }
            symtab.getCompiler().info(
                    "processed math symbol " + ctx.name + " with type "
                            + mathTypes.get(ctx) + typeValueDesc);
        }
        catch (IllegalStateException e) {
            symtab.getCompiler().errorManager.semanticError(
                    ErrorKind.INVALID_MATH_TYPE, ctx.name, ctx.name.getText());
        }
        catch (IllegalArgumentException iae) {
            symtab.getCompiler().errorManager.semanticError(
                    ErrorKind.NO_SUCH_SYMBOL, ctx.name, ctx.name.getText());
        }
    }
}
