package org.resolvelite.proving.absyn;

import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.misc.Nullable;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.resolvelite.parsing.ResolveBaseListener;
import org.resolvelite.parsing.ResolveBaseVisitor;
import org.resolvelite.parsing.ResolveParser;
import org.resolvelite.semantics.MTType;
import org.resolvelite.proving.absyn.PSymbol.PSymbolBuilder;

/**
 * Transforms concrete parse tree math exprs to an equivalent abstract-syntax
 * form, represented by an {@link PExp}.
 */
public class PExpBuildingListener extends ResolveBaseListener {

    private final ParseTreeProperty<MTType> types, typeValues;
    private final ParseTreeProperty<PExp> built = new ParseTreeProperty<>();

    public PExpBuildingListener(ParseTreeProperty<MTType> mathTypes,
            ParseTreeProperty<MTType> mathTypeValues) {
        this.types = mathTypes;
        this.typeValues = mathTypeValues;
    }

    @Nullable public final PExp getBuiltPExp(ParseTree t) {
        return built.get(t);
    }

    @Override public void exitRequiresClause(
            @NotNull ResolveParser.RequiresClauseContext ctx) {
        built.put(ctx, built.get(ctx.mathAssertionExp()));
    }

    @Override public void exitEnsuresClause(
            @NotNull ResolveParser.EnsuresClauseContext ctx) {
        built.put(ctx, built.get(ctx.mathAssertionExp()));
    }

    @Override public void exitMathTypeExp(
            @NotNull ResolveParser.MathTypeExpContext ctx) {
        built.put(ctx, built.get(ctx.mathExp()));
    }

    @Override public void exitMathAssertionExp(
            @NotNull ResolveParser.MathAssertionExpContext ctx) {
        built.put(ctx, built.get(ctx.getChild(0)));
    }

    @Override public void exitMathPrimeExp(
            @NotNull ResolveParser.MathPrimeExpContext ctx) {
        built.put(ctx, built.get(ctx.mathPrimaryExp()));
    }

    @Override public void exitMathPrimaryExp(
            @NotNull ResolveParser.MathPrimaryExpContext ctx) {
        built.put(ctx, built.get(ctx.getChild(0)));
    }

    @Override public void exitMathVariableExp(
            @NotNull ResolveParser.MathVariableExpContext ctx) {
        PSymbolBuilder result = new PSymbolBuilder(ctx.getText()) //
                .incoming(ctx.getParent().getStart().toString().equals("@")) //
                .mathTypeValue(typeValues.get(ctx)) //
                .mathType(types.get(ctx));

        built.put(ctx, result.build());
    }

    @Override public void exitMathBooleanExp(
            @NotNull ResolveParser.MathBooleanExpContext ctx) {
        PSymbolBuilder result = new PSymbolBuilder(ctx.getText()) //
                .mathTypeValue(typeValues.get(ctx)) //
                .mathType(types.get(ctx)) //
                .literal(true);
        built.put(ctx, result.build());
    }
}
