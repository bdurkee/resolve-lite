package edu.clemson.resolve.proving.absyn;

import edu.clemson.resolve.compiler.AnnotatedTree;
import edu.clemson.resolve.misc.Utils;
import edu.clemson.resolve.parser.ResolveBaseListener;
import edu.clemson.resolve.parser.ResolveParser;
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

    @NotNull private final ParseTreeProperty<MTType> types, typeValues;
    @NotNull private final ParseTreeProperty<PTType> progTypes;
    @NotNull private final ParseTreeProperty<PExp> repo;

    @NotNull private final ParseTreeProperty<MTType> seenOperatorTypes =
            new ParseTreeProperty<>();
    @NotNull private final Map<String, Quantification> quantifiedVars =
            new HashMap<>();
    @Nullable private final MTInvalid dummyType;
    private final boolean skipDummyQuantifierNodes;

    /**
     * Constructs a new {@code PExpBuildingListener} given an
     * {@link AnnotatedTree} with it's associated type and expression bindings.
     *
     * @param annotations annotations to be used for constructing expressions
     */
    public PExpBuildingListener(@NotNull AnnotatedTree annotations) {
        this(annotations, null);
    }

    public PExpBuildingListener(@NotNull AnnotatedTree annotations,
                                @Nullable MTInvalid dummyType) {
        this(annotations, dummyType, false);
    }

    /**
     * Constructs a new {@code PExpBuildingListener} given both an
     * {@link AnnotatedTree} and a (possibly-null) dummy type to be used in the
     * case where a 'real' math type is missing from {@code annotations}.
     *
     * @param annotations annotations to be used for constructing expressions
     * @param dummyType an {@link MTInvalid} to be used in place of
     * missing types
     */
    public PExpBuildingListener(@NotNull AnnotatedTree annotations,
                                @Nullable MTInvalid dummyType,
                                boolean skipDummyQuantifiedNodes) {
        this.types = annotations.mathTypes;
        this.typeValues = annotations.mathTypeValues;
        this.progTypes = annotations.progTypes;
        this.repo = annotations.mathPExps;
        this.dummyType = dummyType;
        this.skipDummyQuantifierNodes = skipDummyQuantifiedNodes;
    }

    /** Retrive the final built expr from concrete node {@code t}. */
    @SuppressWarnings("unchecked") public T getBuiltPExp(ParseTree t) {
        return (T) repo.get(t);
    }

    @Override public void exitMathTypeExp(
            ResolveParser.MathTypeExpContext ctx) {
        repo.put(ctx, repo.get(ctx.mathExp()));
    }

    @Override public void exitMathAssertionExp(
            ResolveParser.MathAssertionExpContext ctx) {
        repo.put(ctx, repo.get(ctx.getChild(0)));
    }

    @Override public void exitMathNestedExp(
            ResolveParser.MathNestedExpContext ctx) {
        repo.put(ctx, repo.get(ctx.mathAssertionExp()));
    }

    @Override public void exitMathPrimeExp(
            ResolveParser.MathPrimeExpContext ctx) {
        repo.put(ctx, repo.get(ctx.mathPrimaryExp()));
    }

    @Override public void exitMathPrimaryExp(
            ResolveParser.MathPrimaryExpContext ctx) {
        repo.put(ctx, repo.get(ctx.getChild(0)));
    }

    @Override public void enterMathQuantifiedExp(
            ResolveParser.MathQuantifiedExpContext ctx) {
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
            ResolveParser.MathQuantifiedExpContext ctx) {
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

    @Override public void exitMathUnaryApplyExp(
            ResolveParser.MathUnaryApplyExpContext ctx) {
    }

    @Override public void exitMathPrefixApplyExp(
            ResolveParser.MathPrefixApplyExpContext ctx) {
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
            ResolveParser.MathInfixApplyExpContext ctx) {
        PApplyBuilder result = new PApplyBuilder(buildOperatorPSymbol(ctx, ctx.op))
                .applicationType(getMathType(ctx))
                .applicationTypeValue(getMathTypeValue(ctx))
                .style(PApply.DisplayStyle.INFIX)
                .arguments(Utils.collect(PExp.class, ctx.mathExp(), repo));
        repo.put(ctx, result.build());
        //OK, you're going to need a map from STRING -> MTType for the infix ops.
    }

    @Override public void exitMathOutfixApplyExp(
            ResolveParser.MathOutfixApplyExpContext ctx) {
        PApplyBuilder result =
                new PApplyBuilder(buildOperatorPSymbol(ctx, ctx.lop, ctx.rop))
                    .applicationType(getMathType(ctx))
                    .applicationTypeValue(getMathTypeValue(ctx))
                    .style(PApply.DisplayStyle.OUTFIX)
                    .arguments(repo.get(ctx.mathExp()));
        PApply x = result.build();
        repo.put(ctx, x);
    }

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
            ResolveParser.MathSymbolExpContext ctx) {
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
            ResolveParser.MathLambdaExpContext ctx) {
        List<PLambda.MathSymbolDeclaration> parameters = new ArrayList<>();
        for (ResolveParser.MathVariableDeclGroupContext grp : ctx
                .mathVariableDeclGroup()) {
            for (TerminalNode term : grp.ID()) {
                parameters.add(new PLambda.MathSymbolDeclaration(term.getText(),
                        getMathTypeValue(grp.mathTypeExp())));
            }
        }
        repo.put(ctx, new PLambda(parameters, repo.get(ctx.mathExp())));
    }

    @Override public void exitMathAlternativeExp(
            ResolveParser.MathAlternativeExpContext ctx) {
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
                        getMathType(ctx), getMathTypeValue(ctx));
        repo.put(ctx, result);
    }

    @Override public void exitMathSetExp(ResolveParser.MathSetExpContext ctx) {
        PSet result = new PSet(types.get(ctx), null,
                Utils.collect(PExp.class, ctx.mathExp(), repo));
        repo.put(ctx, result);
    }

    @Override public void exitMathSegmentsExp(
            ResolveParser.MathSegmentsExpContext ctx) {
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
            ResolveParser.MathBooleanLiteralExpContext ctx) {
        PSymbolBuilder result = new PSymbol.PSymbolBuilder(ctx.getText())
                .mathType(getMathType(ctx)).literal(true);
        repo.put(ctx, result.build());
    }

    @Override public void exitMathIntegerLiteralExp(
            ResolveParser.MathIntegerLiteralExpContext ctx) {
        PSymbolBuilder result = new PSymbol.PSymbolBuilder(ctx.getText())
                .mathType(getMathType(ctx)).literal(true);
        repo.put(ctx, result.build());
    }

    @Override public void exitConstraintClause(
            ResolveParser.ConstraintClauseContext ctx) {
        repo.put(ctx, repo.get(ctx.mathAssertionExp()));
    }

    @Override public void exitRequiresClause(
            ResolveParser.RequiresClauseContext ctx) {
        repo.put(ctx, repo.get(ctx.mathAssertionExp()));
    }

    @Override public void exitEnsuresClause(
            ResolveParser.EnsuresClauseContext ctx) {
        repo.put(ctx, repo.get(ctx.mathAssertionExp()));
    }

    @Override public void exitProgPrimaryExp(
            ResolveParser.ProgPrimaryExpContext ctx) {
        repo.put(ctx, repo.get(ctx.progPrimary()));
    }

    @Override public void exitProgPrimary(
            ResolveParser.ProgPrimaryContext ctx) {
        repo.put(ctx, repo.get(ctx.getChild(0)));
    }

    @Override public void exitProgParamExp(
            ResolveParser.ProgParamExpContext ctx) {
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

    @Override public void exitProgVarExp(ResolveParser.ProgVarExpContext ctx) {
        repo.put(ctx, repo.get(ctx.getChild(0)));
    }

    @Override public void exitProgNamedExp(
            ResolveParser.ProgNamedExpContext ctx) {
        PSymbolBuilder result = new PSymbolBuilder(ctx.name.getText())
                .mathTypeValue(getMathTypeValue(ctx))
                .progType(progTypes.get(ctx)).qualifier(ctx.qualifier)
                .mathType(getMathType(ctx));
        repo.put(ctx, result.build());
    }

    @Override public void exitProgNestedExp(
            ResolveParser.ProgNestedExpContext ctx) {
        repo.put(ctx, repo.get(ctx.progExp()));
    }

    @Override public void exitProgBooleanLiteralExp(
            ResolveParser.ProgBooleanLiteralExpContext ctx) {
        repo.put(ctx, buildLiteral(ctx.getText(), types.get(ctx),
                typeValues.get(ctx), progTypes.get(ctx)));
    }

    @Override public void exitProgIntegerLiteralExp(
            ResolveParser.ProgIntegerLiteralExpContext ctx) {
        repo.put(ctx, buildLiteral(ctx.getText(), types.get(ctx),
                typeValues.get(ctx), progTypes.get(ctx)));
    }

    @Override public void exitProgCharacterLiteralExp(
            ResolveParser.ProgCharacterLiteralExpContext ctx) {
        repo.put(ctx, buildLiteral(ctx.getText(), types.get(ctx),
                typeValues.get(ctx), progTypes.get(ctx)));
    }

    @Override public void exitProgStringLiteralExp(
            ResolveParser.ProgStringLiteralExpContext ctx) {
        repo.put(ctx, buildLiteral(ctx.getText(), types.get(ctx),
                typeValues.get(ctx), progTypes.get(ctx)));
    }

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
        return seenOperatorTypes.get(app) == null ? dummyType :
                seenOperatorTypes.get(app);
    }

    private MTType getMathType(ParseTree t) {
        return types.get(t) == null ? dummyType : types.get(t);
    }

    private MTType getMathTypeValue(ParseTree t) {
        return typeValues.get(t) == null ? dummyType : typeValues.get(t);
    }
}
