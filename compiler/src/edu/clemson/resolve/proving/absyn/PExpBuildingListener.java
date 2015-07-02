package edu.clemson.resolve.proving.absyn;

import edu.clemson.resolve.compiler.AnnotatedTree;
import edu.clemson.resolve.misc.Utils;
import edu.clemson.resolve.parser.Resolve;
import edu.clemson.resolve.parser.ResolveBaseListener;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.misc.Nullable;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.rsrg.semantics.MTInvalid;
import org.rsrg.semantics.MTType;
import org.rsrg.semantics.Quantification;
import org.rsrg.semantics.programtype.PTType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Converts parse tree math exprs to an equivalent abstract-syntax form,
 * represented by the {@link PExp} hierarchy.
 */
public class PExpBuildingListener<T extends PExp> extends ResolveBaseListener {

    private final ParseTreeProperty<MTType> types, typeValues;
    private final ParseTreeProperty<PTType> progTypes, progTypeValues;
    private final ParseTreeProperty<PExp> repo;

    private final Map<String, Quantification> quantifiedVars = new HashMap<>();
    private final MTType dummyType;

    public PExpBuildingListener(ParseTreeProperty<PExp> repo,
                                AnnotatedTree annotations) {
        this(repo, annotations, null);
    }

    public PExpBuildingListener(ParseTreeProperty<PExp> repo,
                            AnnotatedTree annotations, MTInvalid dummyType) {
        this.types = annotations.mathTypes;
        this.typeValues = annotations.mathTypeValues;
        this.progTypes = annotations.progTypes;
        this.progTypeValues = annotations.progTypeValues;
        this.repo = repo;
        this.dummyType = dummyType;
    }

    @SuppressWarnings("unchecked") @Nullable public T getBuiltPExp(ParseTree t) {
        return (T) repo.get(t);
    }

    @Override public void exitCorrespondenceClause(
            @NotNull Resolve.CorrespondenceClauseContext ctx) {
        repo.put(ctx, repo.get(ctx.mathAssertionExp()));
    }

    @Override public void exitConstraintClause(
            @NotNull Resolve.ConstraintClauseContext ctx) {
        repo.put(ctx, repo.get(ctx.mathAssertionExp()));
    }

    @Override public void exitConventionClause(
            @NotNull Resolve.ConventionClauseContext ctx) {
        repo.put(ctx, repo.get(ctx.mathAssertionExp()));
    }

    @Override public void exitRequiresClause(
            @NotNull Resolve.RequiresClauseContext ctx) {
        repo.put(ctx, repo.get(ctx.mathAssertionExp()));
    }

    @Override public void exitEnsuresClause(
            @NotNull Resolve.EnsuresClauseContext ctx) {
        repo.put(ctx, repo.get(ctx.mathAssertionExp()));
    }

    @Override public void exitMathTypeAssertionExp(
            @NotNull Resolve.MathTypeAssertionExpContext ctx) {
        repo.put(ctx, repo.get(ctx.mathExp()));
    }

    @Override public void exitMathTypeExp(
            @NotNull Resolve.MathTypeExpContext ctx) {
        repo.put(ctx, repo.get(ctx.mathExp()));
    }

    @Override public void exitMathAssertionExp(
            @NotNull Resolve.MathAssertionExpContext ctx) {
        repo.put(ctx, repo.get(ctx.getChild(0)));
    }

    @Override public void exitMathNestedExp(
            @NotNull Resolve.MathNestedExpContext ctx) {
        repo.put(ctx, repo.get(ctx.mathAssertionExp()));
    }

    @Override public void exitMathPrimeExp(
            @NotNull Resolve.MathPrimeExpContext ctx) {
        repo.put(ctx, repo.get(ctx.mathPrimaryExp()));
    }

    @Override public void exitMathPrimaryExp(
            @NotNull Resolve.MathPrimaryExpContext ctx) {
        repo.put(ctx, repo.get(ctx.getChild(0)));
    }

    @Override public void enterMathQuantifiedExp(
            @NotNull Resolve.MathQuantifiedExpContext ctx) {
        for (TerminalNode term : ctx.mathVariableDeclGroup().ID()) {
            String quantifier = ctx.q.getText();
            quantifiedVars.put(term.getText(),
                    quantifier.equals("Forall") ? Quantification.UNIVERSAL
                            : Quantification.EXISTENTIAL);
        }
    }

    @Override public void exitMathQuantifiedExp(
            @NotNull Resolve.MathQuantifiedExpContext ctx) {
        for (TerminalNode term : ctx.mathVariableDeclGroup().ID()) {
            quantifiedVars.remove(term.getText());
        }
        repo.put(ctx, repo.get(ctx.mathAssertionExp()));
    }

    @Override public void exitMathInfixExp(
            @NotNull Resolve.MathInfixExpContext ctx) {
        PSymbol.PSymbolBuilder result =
                new PSymbol.PSymbolBuilder(ctx.op.getText())
                        .arguments(
                                Utils.collect(PExp.class, ctx.mathExp(), repo))
                        .style(PSymbol.DisplayStyle.INFIX)
                        .mathTypeValue(getMathTypeValue(ctx))
                        .mathType(getMathType(ctx));
        repo.put(ctx, result.build());
    }

    @Override public void exitMathOutfixExp(
            @NotNull Resolve.MathOutfixExpContext ctx) {
        PSymbol.PSymbolBuilder result =
                new PSymbol.PSymbolBuilder(ctx.lop.getText(), ctx.rop.getText()) //
                        .arguments(repo.get(ctx.mathExp())) //
                        .style(PSymbol.DisplayStyle.OUTFIX) //
                        .mathTypeValue(getMathTypeValue(ctx)) //
                        .mathType(getMathType(ctx));
        repo.put(ctx, result.build());
    }

    @Override public void exitMathVariableExp(
            @NotNull Resolve.MathVariableExpContext ctx) {
        PSymbol.PSymbolBuilder result =
                new PSymbol.PSymbolBuilder(ctx.name.getText())
                        .qualifier(ctx.qualifier)
                        .incoming(ctx.AT() != null)
                        .quantification(quantifiedVars.get(ctx.name.getText()))
                        .mathTypeValue(getMathTypeValue(ctx))
                        .mathType(getMathType(ctx));
        repo.put(ctx, result.build());
    }

    @Override public void exitMathLambdaExp(
            @NotNull Resolve.MathLambdaExpContext ctx) {
        List<PLambda.Parameter> parameters = new ArrayList<>();
        for (Resolve.MathVariableDeclGroupContext grp : ctx
                .mathVariableDeclGroup()) {
            for (TerminalNode term : grp.ID()) {
                parameters.add(new PLambda.Parameter(term.getText(),
                        getMathTypeValue(grp.mathTypeExp())));
            }
        }
        repo.put(ctx, new PLambda(parameters, repo.get(ctx.mathExp())));
    }

    @Override public void exitMathAlternativeExp(
            @NotNull Resolve.MathAlternativeExpContext ctx) {
        List<PExp> conditions = new ArrayList<>();
        List<PExp> results = new ArrayList<>();
        PExp otherwiseResult = null;

        for (Resolve.MathAlternativeItemExpContext alt : ctx
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
                        getMathType(ctx), getMathTypeValue(ctx));
        repo.put(ctx, result);
    }

    @Override public void exitMathSegmentsExp(
            @NotNull Resolve.MathSegmentsExpContext ctx) {
        //Todo: Type the individual segs of a seg exp.
        List<PSymbol> segs = ctx.mathFunctionApplicationExp().stream()
                .map(app -> (PSymbol) repo.get(app))
                .collect(Collectors.toList());
        repo.put(ctx, new PSegments(segs, ctx.getStart() //
                .getText().equals("@")));
    }

    @Override public void exitMathFunctionExp(
            @NotNull Resolve.MathFunctionExpContext ctx) {
        List<PExp> s = Utils.collect(PExp.class, ctx.mathExp(), repo);
        PSymbol.PSymbolBuilder result =
                new PSymbol.PSymbolBuilder(ctx.name.getText())
                        .arguments(
                                Utils.collect(PExp.class, ctx.mathExp(), repo))
                        .quantification(quantifiedVars.get(ctx.name.getText()))
                        .mathTypeValue(getMathTypeValue(ctx))
                        .mathType(getMathType(ctx));
        repo.put(ctx, result.build());
    }

    @Override public void exitMathSetCollectionExp(
            @NotNull Resolve.MathSetCollectionExpContext ctx) {
        List<PExp> elements = ctx.mathExp().stream()
                .map(repo::get)
                .collect(Collectors.toList());
        repo.put(ctx, new PSet(getMathType(ctx), getMathTypeValue(ctx), elements));
    }

    @Override public void exitMathBooleanExp(
            @NotNull Resolve.MathBooleanExpContext ctx) {
        PSymbol.PSymbolBuilder result = new PSymbol.PSymbolBuilder(ctx.getText()) //
                .mathType(getMathType(ctx)).literal(true);
        repo.put(ctx, result.build());
    }

    @Override public void exitMathIntegerExp(
            @NotNull Resolve.MathIntegerExpContext ctx) {
        PSymbol.PSymbolBuilder result = new PSymbol.PSymbolBuilder(ctx.getText()) //
                .mathType(getMathType(ctx)).literal(true);
        repo.put(ctx, result.build());
    }

  /*  @Override public void exitProgParamExp(
            @NotNull ResolveParser.ProgParamExpContext ctx) {
        PSymbolBuilder result =
                new PSymbolBuilder(ctx.name.getText())
                        .arguments(
                                Utils.collect(PExp.class, ctx.progExp(), repo))
                        .progType(progTypes.get(ctx)).qualifier(ctx.qualifier)
                        .mathTypeValue(getMathTypeValue(ctx))
                        .mathType(getMathType(ctx));
        repo.put(ctx, result.build());
    }

    @Override public void exitProgApplicationExp(
            @NotNull ResolveParser.ProgApplicationExpContext ctx) {
        PSymbolBuilder result =
                new PSymbolBuilder(Utils.convertProgramOp(ctx.op.getText())
                        .getText())
                        .arguments(
                                Utils.collect(PExp.class, ctx.progExp(), repo))
                        .qualifier("Std_Integer_Fac")
                        .progType(progTypes.get(ctx)) //
                        .mathTypeValue(getMathTypeValue(ctx)) //
                        .mathType(getMathType(ctx));
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
                .mathTypeValue(getMathTypeValue(ctx)) //
                .progType(progTypes.get(ctx)).qualifier(ctx.qualifier) //
                .mathType(getMathType(ctx));
        repo.put(ctx, result.build());
    }

    @Override public void exitProgMemberExp(
            @NotNull ResolveParser.ProgMemberExpContext ctx) {
        List<PSymbol> segs = new ArrayList<>();
        segs.add((PSymbol) repo.get(ctx.getChild(0)));
        for (TerminalNode term : ctx.Identifier()) {
            segs.add(new PSymbol.PSymbolBuilder(term.getText())
                    .mathType(getMathType(term)).progType(progTypes.get(term))
                    .build());
        }
        repo.put(ctx, new PSegments(segs));
    }

    @Override public void exitProgIntegerExp(
            @NotNull ResolveParser.ProgIntegerExpContext ctx) {
        PSymbolBuilder result =
                new PSymbolBuilder(ctx.getText())
                        .mathTypeValue(getMathTypeValue(ctx))
                        .progType(progTypes.get(ctx))
                        .mathType(getMathType(ctx)).literal(true);
        repo.put(ctx, result.build());
    }*/

    private MTType getMathType(ParseTree t) {
        return types.get(t) == null ? dummyType : types.get(t);
    }

    private MTType getMathTypeValue(ParseTree t) {
        return typeValues.get(t) == null ? dummyType : typeValues.get(t);
    }
}
