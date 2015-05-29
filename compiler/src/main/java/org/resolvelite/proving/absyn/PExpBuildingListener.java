package org.resolvelite.proving.absyn;

import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.misc.Nullable;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.resolvelite.compiler.tree.AnnotatedTree;
import org.resolvelite.misc.Utils;
import org.resolvelite.parsing.ResolveBaseListener;
import org.resolvelite.parsing.ResolveParser;
import org.resolvelite.semantics.MTType;
import org.resolvelite.proving.absyn.PSymbol.PSymbolBuilder;
import org.resolvelite.semantics.Quantification;
import org.resolvelite.semantics.programtype.PTType;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Transforms concrete parse tree math exprs to an equivalent abstract-syntax
 * form, represented by an {@link PExp}.
 */
public class PExpBuildingListener<T extends PExp> extends ResolveBaseListener {

    private final ParseTreeProperty<MTType> types, typeValues;
    private final ParseTreeProperty<PTType> progTypes, progTypeValues;
    private final ParseTreeProperty<PExp> repo;

    private final ParseTreeProperty<Quantification> quantifiedExps;

    public PExpBuildingListener(ParseTreeProperty<PExp> repo,
            ParseTreeProperty<Quantification> quantRepo,
            AnnotatedTree annotations) {
        this.types = annotations.mathTypes;
        this.typeValues = annotations.mathTypeValues;
        this.progTypes = annotations.progTypes;
        this.progTypeValues = annotations.progTypeValues;
        this.repo = repo;
        this.quantifiedExps = quantRepo;
    }

    @SuppressWarnings("unchecked") @Nullable public T getBuiltPExp(ParseTree t) {
        return (T) repo.get(t);
    }

    @Override public void exitCorrespondenceClause(
            @NotNull ResolveParser.CorrespondenceClauseContext ctx) {
        repo.put(ctx, repo.get(ctx.mathAssertionExp()));
    }

    @Override public void exitConstraintClause(
            @NotNull ResolveParser.ConstraintClauseContext ctx) {
        repo.put(ctx, repo.get(ctx.mathAssertionExp()));
    }

    @Override public void exitConventionClause(
            @NotNull ResolveParser.ConventionClauseContext ctx) {
        repo.put(ctx, repo.get(ctx.mathAssertionExp()));
    }

    @Override public void exitRequiresClause(
            @NotNull ResolveParser.RequiresClauseContext ctx) {
        repo.put(ctx, repo.get(ctx.mathAssertionExp()));
    }

    @Override public void exitEnsuresClause(
            @NotNull ResolveParser.EnsuresClauseContext ctx) {
        repo.put(ctx, repo.get(ctx.mathAssertionExp()));
    }

    @Override public void exitMathTypeExp(
            @NotNull ResolveParser.MathTypeExpContext ctx) {
        repo.put(ctx, repo.get(ctx.mathExp()));
    }

    @Override public void exitMathAssertionExp(
            @NotNull ResolveParser.MathAssertionExpContext ctx) {
        repo.put(ctx, repo.get(ctx.getChild(0)));
    }

    @Override public void exitMathNestedExp(
            @NotNull ResolveParser.MathNestedExpContext ctx) {
        repo.put(ctx, repo.get(ctx.mathAssertionExp()));
    }

    @Override public void exitMathPrimeExp(
            @NotNull ResolveParser.MathPrimeExpContext ctx) {
        repo.put(ctx, repo.get(ctx.mathPrimaryExp()));
    }

    @Override public void exitMathPrimaryExp(
            @NotNull ResolveParser.MathPrimaryExpContext ctx) {
        repo.put(ctx, repo.get(ctx.getChild(0)));
    }

    @Override public void exitMathInfixExp(
            @NotNull ResolveParser.MathInfixExpContext ctx) {
        PSymbolBuilder result = new PSymbolBuilder(ctx.op.getText()) //
                .arguments(Utils.collect(PExp.class, ctx.mathExp(), repo)) //
                .style(PSymbol.DisplayStyle.INFIX) //
                .mathTypeValue(typeValues.get(ctx)) //
                .mathType(types.get(ctx));
        repo.put(ctx, result.build());
    }

    @Override public void exitMathOutfixExp(
            @NotNull ResolveParser.MathOutfixExpContext ctx) {
        PSymbolBuilder result =
                new PSymbolBuilder(ctx.lop.getText(), ctx.rop.getText()) //
                        .arguments(repo.get(ctx.mathExp())) //
                        .style(PSymbol.DisplayStyle.OUTFIX) //
                        .mathTypeValue(typeValues.get(ctx)) //
                        .mathType(types.get(ctx));
        repo.put(ctx, result.build());
    }

    @Override public void exitMathVariableExp(
            @NotNull ResolveParser.MathVariableExpContext ctx) {
        PSymbolBuilder result = new PSymbolBuilder(ctx.name.getText()) //
                .incoming(ctx.getParent().getStart().toString().equals("@")) //
                .quantification(quantifiedExps.get(ctx)) //
                .mathTypeValue(typeValues.get(ctx)) //
                .mathType(types.get(ctx));
        repo.put(ctx, result.build());
    }

    @Override public void exitMathLambdaExp(
            @NotNull ResolveParser.MathLambdaExpContext ctx) {
        List<PLambda.Parameter> parameters = new ArrayList<>();
        for (ResolveParser.MathVariableDeclGroupContext grp : ctx
                .definitionParameterList().mathVariableDeclGroup()) {
            for (TerminalNode term : grp.Identifier()) {
                parameters.add(new PLambda.Parameter(term.getText(), types
                        .get(grp.mathTypeExp())));
            }
        }
        repo.put(ctx, new PLambda(parameters, repo.get(ctx.mathExp())));
    }

    @Override public void exitMathAlternativeExp(
            @NotNull ResolveParser.MathAlternativeExpContext ctx) {
        List<PExp> conditions = new ArrayList<>();
        List<PExp> results = new ArrayList<>();
        PExp otherwiseResult = null;

        for (ResolveParser.MathAlternativeItemExpContext alt : ctx
                .mathAlternativeItemExp()) {
            if ( alt.condition != null ) {
                conditions.add(repo.get(alt.condition));
                results.add(repo.get(alt.result));
            }
            else {
                otherwiseResult = repo.get(alt.result);
            }
        }
        PAlternatives result =
                new PAlternatives(conditions, results, otherwiseResult,
                        types.get(ctx), typeValues.get(ctx));
        repo.put(ctx, result);
    }

    @Override public void exitMathDotExp(
            @NotNull ResolveParser.MathDotExpContext ctx) {
        List<MTType> segTypes = ctx.Identifier().stream().map(types::get)
                .collect(Collectors.toList());

        List<PSymbol> segs = ctx.Identifier().stream()
                .map(t -> new PSymbolBuilder(t.getText())
                        .mathType(types.get(t)).build())
                .collect(Collectors.toList());
        PSymbolBuilder semanticLast = new PSymbolBuilder(ctx.semantic.getText())
                .mathType(types.get(ctx));

        if (!ctx.mathExp().isEmpty()) {
            semanticLast.arguments(Utils.collect(PExp.class,
                    ctx.mathExp(), repo));
        }
        segs.add(semanticLast.build());
        repo.put(ctx, new PDot(segs, types.get(ctx), typeValues.get(ctx)));
    }

    @Override public void exitMathFunctionExp(
            @NotNull ResolveParser.MathFunctionExpContext ctx) {
        List<PExp> s = Utils.collect(PExp.class, ctx.mathExp(), repo);
        PSymbolBuilder result = new PSymbolBuilder(ctx.name.getText()) //
                .arguments(Utils.collect(PExp.class, ctx.mathExp(), repo))//
                .quantification(quantifiedExps.get(ctx)) //
                .mathTypeValue(typeValues.get(ctx)) //
                .mathType(types.get(ctx));
        repo.put(ctx, result.build());
    }

    @Override public void exitMathSetCollectionExp(
            @NotNull ResolveParser.MathSetCollectionExpContext ctx) {
        List<PExp> elements = ctx.mathExp().stream()
                .map(repo::get)
                .collect(Collectors.toList());
        repo.put(ctx, new PSet(types.get(ctx), typeValues.get(ctx), elements));
    }

    @Override public void exitMathBooleanExp(
            @NotNull ResolveParser.MathBooleanExpContext ctx) {
        PSymbolBuilder result = new PSymbolBuilder(ctx.getText()) //
                .mathType(types.get(ctx)) //
                .literal(true);
        repo.put(ctx, result.build());
    }

    @Override public void exitMathIntegerExp(
            @NotNull ResolveParser.MathIntegerExpContext ctx) {
        PSymbolBuilder result = new PSymbolBuilder(ctx.getText()) //
                .mathType(types.get(ctx)) //
                .literal(true);
        repo.put(ctx, result.build());
    }

    @Override public void exitProgParamExp(
            @NotNull ResolveParser.ProgParamExpContext ctx) {
        PSymbolBuilder result = new PSymbolBuilder(ctx.name.getText()) //
                .arguments(Utils.collect(PExp.class, ctx.progExp(), repo))//
                .progType(progTypes.get(ctx)) //
                .qualifier(ctx.qualifier) //
                .mathTypeValue(typeValues.get(ctx)) //
                .mathType(types.get(ctx));
        repo.put(ctx, result.build());
    }

    @Override public void exitProgPrimaryExp(
            @NotNull ResolveParser.ProgPrimaryExpContext ctx) {
        repo.put(ctx, repo.get(ctx.progPrimary()));
    }

    @Override public void exitProgPrimary(
            @NotNull ResolveParser.ProgPrimaryContext ctx) {
        repo.put(ctx, repo.get(ctx.getChild(0)));
    }

    @Override public void exitProgNamedExp(
            @NotNull ResolveParser.ProgNamedExpContext ctx) {
        PSymbolBuilder result = new PSymbolBuilder(ctx.name.getText()) //
                .mathTypeValue(typeValues.get(ctx)) //
                .progType(progTypes.get(ctx)).qualifier(ctx.qualifier) //
                .mathType(types.get(ctx));
        repo.put(ctx, result.build());
    }

    @Override public void exitProgMemberExp(
            @NotNull ResolveParser.ProgMemberExpContext ctx) {
        List<PSymbol> segs = new ArrayList<>();
        segs.add((PSymbol) repo.get(ctx.getChild(0)));
        for (TerminalNode term : ctx.Identifier()) {
            segs.add(new PSymbol.PSymbolBuilder(term.getText())
                    .mathType(types.get(term)).progType(progTypes.get(term))
                    .build());
        }
        PDot result =
                new PDot(segs, types.get(ctx), typeValues.get(ctx),
                        progTypes.get(ctx), progTypeValues.get(ctx));
        repo.put(ctx, result);
    }

    @Override public void exitProgIntegerExp(
            @NotNull ResolveParser.ProgIntegerExpContext ctx) {
        PSymbolBuilder result = new PSymbolBuilder(ctx.getText()) //
                .mathTypeValue(typeValues.get(ctx)) //
                .progType(progTypes.get(ctx)) //
                .mathType(types.get(ctx)) //
                .literal(true);
        repo.put(ctx, result.build());
    }
}
