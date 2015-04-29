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

import java.util.List;
import java.util.stream.Collectors;

/**
 * Transforms concrete parse tree math exprs to an equivalent abstract-syntax
 * form, represented by an {@link PExp}.
 */
public class PExpBuildingListener<T extends PExp> extends ResolveBaseListener {

    private final ParseTreeProperty<MTType> types, typeValues;
    private final ParseTreeProperty<PExp> built = new ParseTreeProperty<>();

    public PExpBuildingListener(ParseTreeProperty<MTType> mathTypes,
            ParseTreeProperty<MTType> mathTypeValues) {
        this.types = mathTypes;
        this.typeValues = mathTypeValues;
    }

    @SuppressWarnings("unchecked") @Nullable public T getBuiltPExp(ParseTree t) {
        return (T) built.get(t);
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

    @Override public void exitMathInfixExp(
            @NotNull ResolveParser.MathInfixExpContext ctx) {
        PSymbolBuilder result = new PSymbolBuilder(ctx.op.getText()) //
                .arguments(collectPExpsFor(PExp.class, ctx.mathExp())) //
                .style(PSymbol.DisplayStyle.INFIX) //
                .mathTypeValue(typeValues.get(ctx)) //
                .mathType(types.get(ctx));
        built.put(ctx, result.build());
    }

    @Override public void exitMathVariableExp(
            @NotNull ResolveParser.MathVariableExpContext ctx) {
        PSymbolBuilder result = new PSymbolBuilder(ctx.getText()) //
                .incoming(ctx.getParent().getStart().toString().equals("@")) //
                .mathTypeValue(typeValues.get(ctx)) //
                .mathType(types.get(ctx));
        built.put(ctx, result.build());
    }

    @Override public void exitMathFunctionExp(
            @NotNull ResolveParser.MathFunctionExpContext ctx) {
        PSymbolBuilder result = new PSymbolBuilder(ctx.getText()) //
                .arguments(collectPExpsFor(PExp.class, ctx.mathExp(), built))//
                .mathTypeValue(typeValues.get(ctx)) //
                .mathType(types.get(ctx));
        built.put(ctx, result.build());
    }

    @Override public void exitMathBooleanExp(
            @NotNull ResolveParser.MathBooleanExpContext ctx) {
        PSymbolBuilder result = new PSymbolBuilder(ctx.getText()) //
                .mathType(types.get(ctx)) //
                .literal(true);
        built.put(ctx, result.build());
    }

    private <E extends PExp> List<E> collectPExpsFor(Class<E> expectedExpType,
            List<? extends ParseTree> nodes) {
        return collectPExpsFor(expectedExpType, nodes, built);
    }

    public static <E extends PExp> List<E> collectPExpsFor(
            Class<E> expectedExpType, List<? extends ParseTree> nodes,
            ParseTreeProperty<PExp> annotations) {
        return nodes.stream().map(x -> expectedExpType
                .cast(annotations.get(x))).collect(Collectors.toList());
    }
}
