package org.resolvelite.proving.absyn;

import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.misc.Nullable;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.resolvelite.misc.Utils;
import org.resolvelite.parsing.ResolveBaseListener;
import org.resolvelite.parsing.ResolveBaseVisitor;
import org.resolvelite.parsing.ResolveParser;
import org.resolvelite.semantics.MTType;
import org.resolvelite.proving.absyn.PSymbol.PSymbolBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Transforms concrete parse tree math exprs to an equivalent abstract-syntax
 * form, represented by an {@link PExp}.
 */
public class PExpBuildingListener<T extends PExp> extends ResolveBaseListener {

    private final ParseTreeProperty<MTType> types, typeValues;
    private final ParseTreeProperty<PExp> built;

    public PExpBuildingListener(ParseTreeProperty<PExp> repo,
            ParseTreeProperty<MTType> mathTypes,
            ParseTreeProperty<MTType> mathTypeValues) {
        this.types = mathTypes;
        this.typeValues = mathTypeValues;
        this.built = repo;
    }

    @SuppressWarnings("unchecked") @Nullable public T getBuiltPExp(ParseTree t) {
        return (T) built.get(t);
    }

    @Override public void exitConstraintClause(
            @NotNull ResolveParser.ConstraintClauseContext ctx) {
        built.put(ctx, built.get(ctx.mathAssertionExp()));
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
                .arguments(Utils.collect(PExp.class, ctx.mathExp(), built)) //
                .style(PSymbol.DisplayStyle.INFIX) //
                .mathTypeValue(typeValues.get(ctx)) //
                .mathType(types.get(ctx));
        built.put(ctx, result.build());
    }

    @Override public void exitMathVariableExp(
            @NotNull ResolveParser.MathVariableExpContext ctx) {
        PSymbolBuilder result = new PSymbolBuilder(ctx.name.getText()) //
                .incoming(ctx.getParent().getStart().toString().equals("@")) //
                .mathTypeValue(typeValues.get(ctx)) //
                .mathType(types.get(ctx));
        built.put(ctx, result.build());
    }

    @Override public void exitMathFunctionExp(
            @NotNull ResolveParser.MathFunctionExpContext ctx) {
        PSymbolBuilder result = new PSymbolBuilder(ctx.name.getText()) //
                .arguments(Utils.collect(PExp.class, ctx.mathExp(), built))//
                .mathTypeValue(typeValues.get(ctx)) //
                .mathType(types.get(ctx));
        built.put(ctx, result.build());
    }

    @Override public void exitMathSetCollectionExp(
            @NotNull ResolveParser.MathSetCollectionExpContext ctx) {
        List<PExp> elements = ctx.mathExp().stream()
                .map(built::get)
                .collect(Collectors.toList());
        built.put(ctx, new PSet(types.get(ctx), typeValues.get(ctx), elements));
    }

    @Override public void exitMathBooleanExp(
            @NotNull ResolveParser.MathBooleanExpContext ctx) {
        PSymbolBuilder result = new PSymbolBuilder(ctx.getText()) //
                .mathType(types.get(ctx)) //
                .literal(true);
        built.put(ctx, result.build());
    }

    @Override public void exitMathIntegerExp(
            @NotNull ResolveParser.MathIntegerExpContext ctx) {
        PSymbolBuilder result = new PSymbolBuilder(ctx.getText()) //
                .mathType(types.get(ctx)) //
                .literal(true);
        built.put(ctx, result.build());
    }

    @Override public void exitProgParamExp(
            @NotNull ResolveParser.ProgParamExpContext ctx) {
        PSymbolBuilder result = new PSymbolBuilder(ctx.name.getText()) //
                .arguments(Utils.collect(PExp.class, ctx.progExp(), built))//
                .mathTypeValue(typeValues.get(ctx)) //
                .mathType(types.get(ctx));
        built.put(ctx, result.build());
    }

    @Override public void exitProgPrimaryExp(
            @NotNull ResolveParser.ProgPrimaryExpContext ctx) {
        built.put(ctx, built.get(ctx.progPrimary()));
    }

    @Override public void exitProgPrimary(
            @NotNull ResolveParser.ProgPrimaryContext ctx) {
        built.put(ctx, built.get(ctx.getChild(0)));
    }

    @Override public void exitProgNamedExp(
            @NotNull ResolveParser.ProgNamedExpContext ctx) {
        PSymbolBuilder result = new PSymbolBuilder(ctx.name.getText()) //
                .mathTypeValue(typeValues.get(ctx)) //
                .mathType(types.get(ctx));
        built.put(ctx, result.build());
    }

    @Override public void exitProgIntegerExp(
            @NotNull ResolveParser.ProgIntegerExpContext ctx) {
        PSymbolBuilder result = new PSymbolBuilder(ctx.getText()) //
                .mathTypeValue(typeValues.get(ctx)) //
                .mathType(types.get(ctx))   //
                .literal(true);
        built.put(ctx, result.build());
    }
}
