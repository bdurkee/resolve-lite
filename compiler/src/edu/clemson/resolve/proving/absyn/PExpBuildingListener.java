package edu.clemson.resolve.proving.absyn;

import edu.clemson.resolve.compiler.AnnotatedModule;
import edu.clemson.resolve.misc.Utils;
import edu.clemson.resolve.parser.Resolve;
import edu.clemson.resolve.parser.ResolveBaseListener;
import edu.clemson.resolve.proving.absyn.PSymbol.PSymbolBuilder;
import edu.clemson.resolve.proving.absyn.PApply.PApplyBuilder;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rsrg.semantics.*;
import org.rsrg.semantics.MTFunction.MTFunctionBuilder;
import org.rsrg.semantics.programtype.PTType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Converts parse tree math exprs to an equivalent abstract-syntax form,
 * represented by the {@link PExp} hierarchy.
 */
public class PExpBuildingListener<T extends PExp> extends ResolveBaseListener {

    @NotNull private final AnnotatedModule annotations;
    @NotNull private final ParseTreeProperty<PExp> repo;

    @NotNull private final Map<String, Quantification> quantifiedVars =
            new HashMap<>();
    private final boolean skipDummyQuantifierNodes;
    @NotNull private final TypeGraph g;

    /**
     * Constructs a new {@code PExpBuildingListener} given an
     * {@link AnnotatedModule} with it's associated type and expression bindings.
     *
     * @param g a typegraph
     * @param annotations annotations to be used for constructing expressions
     */
    public PExpBuildingListener(@NotNull TypeGraph g,
                                @NotNull AnnotatedModule annotations) {
        this(g, annotations, false);
    }

    /**
     * Constructs a new {@code PExpBuildingListener} given an instance of
     * {@link TypeGraph}, some module {@code annotations} and a boolean flag
     * indicating whether or not to construct special syntactic nodes that
     * pair an arbitrary number of quantified bound variables with a
     * {@code PExp}s.
     *
     * @param annotations annotations to be used for constructing expressions
     */
    public PExpBuildingListener(@NotNull TypeGraph g,
                                @NotNull AnnotatedModule annotations,
                                boolean skipDummyQuantifiedNodes) {
        this.g = g;
        this.annotations = annotations;
        this.skipDummyQuantifierNodes = skipDummyQuantifiedNodes;
        this.repo = annotations.mathPExps;
    }

    /** Retrive the final built expr from concrete node {@code t}. */
    @SuppressWarnings("unchecked") public T getBuiltPExp(ParseTree t) {
        return (T) repo.get(t);
    }

    @Override public void exitMathTypeExp(Resolve.MathTypeExpContext ctx) {
        repo.put(ctx, repo.get(ctx.mathExp()));
    }

    @Override public void exitMathAssertionExp(
            Resolve.MathAssertionExpContext ctx) {
        repo.put(ctx, repo.get(ctx.getChild(0)));
    }

    @Override public void exitMathTypeAssertionExp(
            Resolve.MathTypeAssertionExpContext ctx) {
        repo.put(ctx, repo.get(ctx.mathExp()));
    }

    @Override public void exitMathNestedExp(Resolve.MathNestedExpContext ctx) {
        repo.put(ctx, repo.get(ctx.mathAssertionExp()));
    }

    @Override public void exitMathPrimeExp(Resolve.MathPrimeExpContext ctx) {
        repo.put(ctx, repo.get(ctx.mathPrimaryExp()));
    }

    @Override public void exitMathPrimaryExp(Resolve.MathPrimaryExpContext ctx) {
        repo.put(ctx, repo.get(ctx.getChild(0)));
    }

    @Override public void enterMathQuantifiedExp(
            Resolve.MathQuantifiedExpContext ctx) {
        for (TerminalNode term : ctx.mathVariableDeclGroup().ID()) {
            String quantifier = ctx.q.getText();
            quantifiedVars.put(term.getText(),
                    getQuantificationMode(ctx.q));
        }
    }

    public Quantification getQuantificationMode(@NotNull Token q) {
        Quantification result = Quantification.NONE;
        if (q.getText().equalsIgnoreCase("forall")) {
            result = Quantification.UNIVERSAL;
        }
        else {
            result = Quantification.EXISTENTIAL;
        }
        return result;
    }


    @Override public void exitMathQuantifiedExp(
            Resolve.MathQuantifiedExpContext ctx) {
        List<PLambda.MathSymbolDeclaration> declaredVars =
                new ArrayList<>();
        for (TerminalNode term : ctx.mathVariableDeclGroup().ID()) {
            quantifiedVars.remove(term.getText());
            declaredVars.add(new PLambda.MathSymbolDeclaration(term.getText(),
                    getMathTypeValue(ctx.mathVariableDeclGroup().mathTypeExp())));
        }
        PQuantified q = new PQuantified(repo.get(ctx.mathAssertionExp()),
                getQuantificationMode(ctx.q), declaredVars);
        if (skipDummyQuantifierNodes) {
            repo.put(ctx, repo.get(ctx.mathAssertionExp()));
        }
        else {
            repo.put(ctx, q);
        }
    }

    @Override public void exitMathPrefixApplyExp(
            Resolve.MathPrefixApplyExpContext ctx) {
        List<? extends ParseTree> args = ctx.mathExp()
                .subList(1, ctx.mathExp().size());
        PApplyBuilder result = new PApplyBuilder(repo.get(ctx.functionExp))
                .arguments(Utils.collect(PExp.class, args, repo))
                .applicationType(getMathType(ctx))
                .style(PApply.DisplayStyle.PREFIX)
                .applicationTypeValue(getMathTypeValue(ctx));
        repo.put(ctx, result.build());
    }

    @Override public void exitMathInfixApplyExp(
            Resolve.MathInfixApplyExpContext ctx) {
        PApplyBuilder result = new PApplyBuilder(buildOperatorPSymbol(ctx, ctx.op))
                .applicationType(getMathType(ctx))
                .applicationTypeValue(getMathTypeValue(ctx))
                .style(PApply.DisplayStyle.INFIX)
                .arguments(Utils.collect(PExp.class, ctx.mathExp(), repo));
        repo.put(ctx, result.build());
        //OK, you're going to need a map from STRING -> MTType for the infix ops.
    }

    /*@Override public void exitMathOutfixApplyExp(
            Resolve.MathOutfixApplyExpContext ctx) {
        PApplyBuilder result =
                new PApplyBuilder(buildOperatorPSymbol(ctx, ctx.lop, ctx.rop))
                    .applicationType(getMathType(ctx))
                    .applicationTypeValue(getMathTypeValue(ctx))
                    .style(PApply.DisplayStyle.OUTFIX)
                    .arguments(repo.get(ctx.mathExp()));
        PApply x = result.build();
        repo.put(ctx, x);
    }*/

    private PSymbol buildOperatorPSymbol(ParserRuleContext app,
                                         Token lop, Token rop) {
        return new PSymbolBuilder(lop.getText(), rop.getText())
                .mathType(getMathType(app))
                .build();
    }

    private PSymbol buildOperatorPSymbol(ParserRuleContext app,
                                         Token operator) {
        return buildOperatorPSymbol(app, operator.getText());
    }

    private PSymbol buildOperatorPSymbol(ParserRuleContext app,
                                         String operator) {
        return new PSymbolBuilder(operator)
                .mathType(getMathType(app))
                .quantification(quantifiedVars.get(operator))
                .build();
    }

    @Override public void exitMathSymbolExp(
            Resolve.MathSymbolExpContext ctx) {
        MTType t = getMathType(ctx);
        PSymbolBuilder result = new PSymbolBuilder(ctx.name.getText())
                .qualifier(ctx.qualifier)
                .incoming(ctx.incoming != null)
                .quantification(quantifiedVars.get(ctx.name.getText()))
                .mathTypeValue(getMathTypeValue(ctx))
                .mathType(getMathType(ctx));
        repo.put(ctx, result.build());
    }

    @Override public void exitMathLambdaExp(
            Resolve.MathLambdaExpContext ctx) {
        List<PLambda.MathSymbolDeclaration> parameters = new ArrayList<>();
        for (Resolve.MathVariableDeclGroupContext grp : ctx
                .mathVariableDeclGroup()) {
            for (TerminalNode term : grp.ID()) {
                parameters.add(new PLambda.MathSymbolDeclaration(term.getText(),
                        getMathTypeValue(grp.mathTypeExp())));
            }
        }
        repo.put(ctx, new PLambda(parameters, repo.get(ctx.mathExp())));
    }

    @Override public void exitMathAlternativeExp(
            Resolve.MathAlternativeExpContext ctx) {
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

    @Override public void exitMathSetExp(Resolve.MathSetExpContext ctx) {
        repo.put(ctx, new PSet(annotations.mathTypes.get(ctx), null,
                Utils.collect(PExp.class, ctx.mathExp(), repo)));
    }

    @Override public void exitMathSegmentsExp(
            Resolve.MathSegmentsExpContext ctx) {
        List<String> nameComponents = ctx.mathSymbolExp().stream()
                .map(app -> repo.get(app).getCanonicalName())
                .collect(Collectors.toList());
        PExp last = repo.get(ctx.mathSymbolExp()
                .get(ctx.mathSymbolExp().size() - 1));
        PExp first = repo.get(ctx.mathSymbolExp().get(0));
        PExp result = new PSymbolBuilder(Utils.join(nameComponents, "."))
                .mathType(last.getMathType()).incoming(first.isIncoming())
                .build();

        if (!ctx.mathExp().isEmpty()) {
             result = new PApplyBuilder(result)
                    .arguments(Utils.collect(PExp.class, ctx.mathExp(), repo))
                    .applicationType(last.getMathType()).build();
        }
        repo.put(ctx, result);
    }

    @Override public void exitMathBooleanLiteralExp(
            Resolve.MathBooleanLiteralExpContext ctx) {
        PSymbolBuilder result = new PSymbol.PSymbolBuilder(ctx.getText())
                .mathType(getMathType(ctx)).literal(true);
        repo.put(ctx, result.build());
    }

    @Override public void exitMathIntegerLiteralExp(
            Resolve.MathIntegerLiteralExpContext ctx) {
        PSymbolBuilder result = new PSymbol.PSymbolBuilder(ctx.getText())
                .mathType(getMathType(ctx)).literal(true);
        repo.put(ctx, result.build());
    }

    @Override public void exitConstraintClause(
            Resolve.ConstraintClauseContext ctx) {
        repo.put(ctx, repo.get(ctx.mathAssertionExp()));
    }

    @Override public void exitRequiresClause(
            Resolve.RequiresClauseContext ctx) {
        repo.put(ctx, repo.get(ctx.mathAssertionExp()));
    }

    @Override public void exitEnsuresClause(
            Resolve.EnsuresClauseContext ctx) {
        repo.put(ctx, repo.get(ctx.mathAssertionExp()));
    }

    /*@Override public void exitProgPrimaryExp(
            Resolve.ProgPrimaryExpContext ctx) {
        repo.put(ctx, repo.get(ctx.progPrimary()));
    }

    @Override public void exitProgPrimary(
            Resolve.ProgPrimaryContext ctx) {
        repo.put(ctx, repo.get(ctx.getChild(0)));
    }

    @Override public void exitProgParamExp(
            Resolve.ProgParamExpContext ctx) {
        MTFunction mathType = fakeFunctionType(ctx.progExp(), types.get(ctx));
        PSymbol namePortion = new PSymbolBuilder(ctx.name.getText())
                .progType(progTypes.get(ctx)).qualifier(ctx.qualifier)
                .mathType(mathType)
                .qualifier(ctx.qualifier)
                .build();
        PApplyBuilder result = new PApplyBuilder(namePortion)
                .arguments(Utils.collect(PExp.class, ctx.progExp(), repo))
                .applicationType(types.get(ctx));
        repo.put(ctx, result.build());
    }

    //TODO: Until I come up with a palatable way of passing this info (already
    //formed) into this builder, I'll just reconstruct the MTFunction manually
    //from the types of the arguments for now.
    private MTFunction fakeFunctionType(@NotNull List<? extends ParseTree> args,
                                        @NotNull MTType retType) {
        List<MTType> argMathTypes = args.stream()
                .map(t -> repo.get(t).getMathType())
                .collect(Collectors.toList());
        return new MTFunctionBuilder(retType.getTypeGraph(), retType)
                .paramTypes(argMathTypes).build();
    }

    @Override public void exitProgVarExp(Resolve.ProgVarExpContext ctx) {
        repo.put(ctx, repo.get(ctx.getChild(0)));
    }

    @Override public void exitProgNamedExp(
            Resolve.ProgNamedExpContext ctx) {
        PSymbolBuilder result = new PSymbolBuilder(ctx.name.getText())
                .mathTypeValue(getMathTypeValue(ctx))
                .progType(progTypes.get(ctx)).qualifier(ctx.qualifier)
                .mathType(getMathType(ctx));
        repo.put(ctx, result.build());
    }

    @Override public void exitProgNestedExp(
            Resolve.ProgNestedExpContext ctx) {
        repo.put(ctx, repo.get(ctx.progExp()));
    }

    @Override public void exitProgBooleanLiteralExp(
            Resolve.ProgBooleanLiteralExpContext ctx) {
        repo.put(ctx, buildLiteral(ctx.getText(), types.get(ctx),
                typeValues.get(ctx), progTypes.get(ctx)));
    }

    @Override public void exitProgIntegerLiteralExp(
            Resolve.ProgIntegerLiteralExpContext ctx) {
        repo.put(ctx, buildLiteral(ctx.getText(), types.get(ctx),
                typeValues.get(ctx), progTypes.get(ctx)));
    }

    @Override public void exitProgCharacterLiteralExp(
            Resolve.ProgCharacterLiteralExpContext ctx) {
        repo.put(ctx, buildLiteral(ctx.getText(), types.get(ctx),
                typeValues.get(ctx), progTypes.get(ctx)));
    }

    @Override public void exitProgStringLiteralExp(
            Resolve.ProgStringLiteralExpContext ctx) {
        repo.put(ctx, buildLiteral(ctx.getText(), types.get(ctx),
                typeValues.get(ctx), progTypes.get(ctx)));
    }*/

    private PExp buildLiteral(String literalText, MTType type, MTType typeValue,
                              PTType progType) {
        PSymbol.PSymbolBuilder result =
                new PSymbolBuilder(literalText).mathType(type)
                        .progType(progType).mathTypeValue(typeValue)
                        .literal(true);
        return result.build();
    }

    //this should probably actually always return MTFunction...
    private MTType getOperandFunctionType(ParserRuleContext app) {
        //return seenOperatorTypes.get(app) == null ? MTInvalid.getInstance(g) :
        //        seenOperatorTypes.get(app);
        return null;
    }

    private MTType getMathType(ParseTree t) {
        return annotations.mathTypes.get(t) == null ? g.INVALID :
                annotations.mathTypes.get(t);
    }

    private MTType getMathTypeValue(ParseTree t) {
        return annotations.mathTypeValues.get(t) == null ? g.INVALID :
                annotations.mathTypeValues.get(t);
    }
}
