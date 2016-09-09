package edu.clemson.resolve.proving.absyn;

import edu.clemson.resolve.compiler.AnnotatedModule;
import edu.clemson.resolve.misc.StdTemplateProgOps;
import edu.clemson.resolve.misc.Utils;
import edu.clemson.resolve.parser.ResolveLexer;
import edu.clemson.resolve.parser.ResolveParser;
import edu.clemson.resolve.parser.ResolveBaseListener;
import edu.clemson.resolve.proving.absyn.PApply.PApplyBuilder;
import edu.clemson.resolve.proving.absyn.PSymbol.PSymbolBuilder;
import edu.clemson.resolve.semantics.DumbMathClssftnHandler;
import edu.clemson.resolve.semantics.MathClssftn;
import edu.clemson.resolve.semantics.Quantification;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import edu.clemson.resolve.semantics.programtype.ProgType;

import java.util.*;

import static edu.clemson.resolve.proving.absyn.PApply.DisplayStyle.*;

/**
 * Converts parse tree math exprs to an equivalent abstract-syntax form, represented by the {@link PExp} hierarchy.
 * Get the final, built {@link PExp} via a call to {@link #getBuiltPExp(ParseTree)}.
 */
public class PExpBuildingListener<T extends PExp> extends ResolveBaseListener {

    private final AnnotatedModule annotations;
    private final ParseTreeProperty<PExp> repo;

    private final Map<String, Quantification> quantifiedVars = new HashMap<>();
    private final boolean skipDummyQuantifierNodes;
    private final DumbMathClssftnHandler g;

    /**
     * Constructs a new {@code PExpBuildingListener} given an {@link AnnotatedModule} with it's associated expression
     * tree mappings.
     *
     * @param g           a clssftnhandler
     * @param annotations annotations to be used for constructing expressions
     */
    public PExpBuildingListener(@NotNull DumbMathClssftnHandler g, @NotNull AnnotatedModule annotations) {
        this(g, annotations, true);
    }

    /**
     * Constructs a new {@code PExpBuildingListener} given an instance of {@link DumbMathClssftnHandler}, some module
     * {@code annotations} and a boolean flag {@code skipDummyQuantifiedNodes} indicating whether or not to construct
     * special, explicit syntactic node representing existential or universal quantifiers.
     *
     * @param annotations annotations to be used for constructing expressions
     */
    public PExpBuildingListener(@NotNull DumbMathClssftnHandler g,
                                @NotNull AnnotatedModule annotations,
                                boolean skipDummyQuantifiedNodes) {
        this.g = g;
        this.annotations = annotations;
        this.skipDummyQuantifierNodes = skipDummyQuantifiedNodes;
        this.repo = annotations.exprASTs;
    }

    /**
     * Retrive the final built expr from concrete node {@code t}.
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public T getBuiltPExp(ParseTree t) {
        return (T) repo.get(t);
    }

    @Override
    public void exitMathClssftnExp(ResolveParser.MathClssftnExpContext ctx) {
        repo.put(ctx, repo.get(ctx.mathExp()));
    }

    @Override
    public void exitMathAssertionExp(ResolveParser.MathAssertionExpContext ctx) {
        repo.put(ctx, repo.get(ctx.getChild(0)));
    }

    @Override
    public void exitMathClssftnAssertionExp(ResolveParser.MathClssftnAssertionExpContext ctx) {
        repo.put(ctx, repo.get(ctx.mathExp(0)));
    }

    @Override
    public void exitMathNestedExp(ResolveParser.MathNestedExpContext ctx) {
        repo.put(ctx, repo.get(ctx.mathAssertionExp()));
    }

    @Override
    public void exitMathPrimeExp(ResolveParser.MathPrimeExpContext ctx) {
        repo.put(ctx, repo.get(ctx.getChild(0)));
    }

    @Override
    public void exitMathPrimaryExp(ResolveParser.MathPrimaryExpContext ctx) {
        repo.put(ctx, repo.get(ctx.mathPrimeExp()));
    }

    @Override
    public void enterMathQuantifiedExp(ResolveParser.MathQuantifiedExpContext ctx) {
        for (ResolveParser.MathSymbolNameContext term : ctx.mathVarDeclGroup().mathSymbolName()) {
            String quantifier = ctx.q.getText();
            quantifiedVars.put(term.getText(), getQuantificationMode(ctx.q));
        }
    }

    public Quantification getQuantificationMode(@NotNull Token q) {
        Quantification result = Quantification.NONE;
        switch (q.getType()) {
            case ResolveLexer.FORALL:
                result = Quantification.UNIVERSAL;
                break;
            case ResolveLexer.EXISTS:
                result = Quantification.EXISTENTIAL;
                break;
        }
        return result;
    }

    @Override
    public void exitMathQuantifiedExp(ResolveParser.MathQuantifiedExpContext ctx) {
        List<PLambda.MathSymbolDeclaration> declaredVars = new ArrayList<>();
        for (ResolveParser.MathSymbolNameContext term : ctx.mathVarDeclGroup().mathSymbolName()) {
            quantifiedVars.remove(term.getText());
            declaredVars.add(new PLambda.MathSymbolDeclaration(term.getText(),
                    getMathClssfctn(ctx.mathVarDeclGroup().mathClssftnExp())));
        }
        PQuantified q = new PQuantified(repo.get(ctx.mathAssertionExp()), getQuantificationMode(ctx.q), declaredVars);
        if (skipDummyQuantifierNodes) {
            repo.put(ctx, repo.get(ctx.mathAssertionExp()));
        }
        else {
            repo.put(ctx, q);
        }
    }

    @Override
    public void exitMathPrefixAppExp(ResolveParser.MathPrefixAppExpContext ctx) {
        List<? extends ParseTree> args = ctx.mathExp().subList(1, ctx.mathExp().size());
        PApplyBuilder result =
                new PApplyBuilder(repo.get(ctx.name))
                        .arguments(Utils.collect(PExp.class, args, repo))
                        .applicationType(getMathClssfctn(ctx))
                        .style(PREFIX);
        repo.put(ctx, result.build());
    }

    @Override
    public void exitMathInfixAppExp(ResolveParser.MathInfixAppExpContext ctx) {
        PExp left = repo.get(ctx.mathExp(0));
        PExp right = repo.get(ctx.mathExp(1));

        //hardcode hook to handle chained relationals (generalize this at the syntax level with some
        //special syntax)
        if (annotations.chainableCtx(ctx) && annotations.chainableCtx(ctx.mathExp(0))) {
            PExp left2 = getBottommostFormula(left);
            PApply newRight = new PApplyBuilder((PSymbol) repo.get(ctx.getChild(1)))
                    .applicationType(getMathClssfctn(ctx))
                    .style(INFIX)
                    .arguments(left2.getSubExpressions().get(2), right).build();
            PApply result = g.formConjunct(left, newRight);
            repo.put(ctx, result);
        }
        else {
            PApplyBuilder result = new PApplyBuilder((PSymbol) repo.get(ctx.getChild(1)))
                    .applicationType(getMathClssfctn(ctx))
                    .style(INFIX)
                    .arguments(Utils.collect(PExp.class, ctx.mathExp(), repo));
            repo.put(ctx, result.build());
        }
    }

    @NotNull
    public PExp getBottommostFormula(@NotNull PExp l) {
        PExp result = l;
        while ((result.getTopLevelOperationName().equals("and") ||
                result.getTopLevelOperationName().equals("∧")) && result instanceof PApply) {
            result = ((PApply) result).getArguments().get(1); //keep getting rhs
        }
        return result;
    }

    @Override
    public void exitMathOutfixAppExp(ResolveParser.MathOutfixAppExpContext ctx) {
        PSymbol operator = new PSymbolBuilder(ctx.lop.getText(), ctx.rop.getText())
                .mathClssfctn(getMathClssfctn(ctx.lop))
                .quantification(quantifiedVars.get(ctx.lop.getText()))
                .build();
        PApplyBuilder result =
                new PApplyBuilder(operator)
                        .applicationType(getMathClssfctn(ctx)).style(OUTFIX)
                        .arguments(repo.get(ctx.mathExp()));
        repo.put(ctx, result.build());
    }
/*
    @Override
    public void exitMathBracketAppExp(ResolveParser.MathBracketAppExpContext ctx) {
        PApplyBuilder result =
                new PApplyBuilder((PSymbol) repo.get(ctx.mathSqBrOpExp()))
                        .applicationType(getMathClssfctn(ctx))
                        .style(PREFIX, true)
                        .arguments(Utils.collect(PExp.class, ctx.mathExp(), repo));
        repo.put(ctx, result.build());
    }*/

    @Override
    public void exitMathSymbolExp(ResolveParser.MathSymbolExpContext ctx) {
        MathClssftn t = getMathClssfctn(ctx);
        PSymbolBuilder result =
                new PSymbolBuilder(ctx.name.getText())
                        .qualifier(ctx.qualifier)
                        .incoming(ctx.incoming != null)
                        .quantification(quantifiedVars.get(ctx.name.getText()))
                        .mathClssfctn(getMathClssfctn(ctx));
        repo.put(ctx, result.build());
    }

    @Override
    public void exitMathLambdaExp(ResolveParser.MathLambdaExpContext ctx) {
        List<PLambda.MathSymbolDeclaration> parameters = new ArrayList<>();
        parameters.add(new PLambda.MathSymbolDeclaration(ctx.mathVarDecl().mathSymbolName().getText(), getMathClssfctn(ctx)));
        repo.put(ctx, new PLambda(parameters, repo.get(ctx.mathExp())));
    }

    @Override
    public void exitMathAlternativeExp(ResolveParser.MathAlternativeExpContext ctx) {
        List<PExp> conditions = new ArrayList<>();
        List<PExp> results = new ArrayList<>();
        PExp otherwiseResult = null;

        for (ResolveParser.MathAlternativeItemExpContext alt : ctx
                .mathAlternativeItemExp()) {
            if (alt.condition != null) {
                conditions.add(repo.get(alt.condition));
                results.add(repo.get(alt.result));
            }
            else {
                otherwiseResult = repo.get(alt.result);
            }
        }
        MathClssftn x = getMathClssfctn(ctx);
        PAlternatives result = new PAlternatives(conditions, results, otherwiseResult, getMathClssfctn(ctx));
        repo.put(ctx, result);
    }

    @Override
    public void exitMathSetExp(ResolveParser.MathSetExpContext ctx) {
        repo.put(ctx, new PSet(annotations.mathClssftns.get(ctx), Utils.collect(PExp.class, ctx.mathExp(), repo)));
    }

    @Override
    public void exitMathSelectorExp(ResolveParser.MathSelectorExpContext ctx) {
        repo.put(ctx, new PSelector(repo.get(ctx.lhs), repo.get(ctx.rhs)));
    }

    @Override
    public void exitConstraintsClause(ResolveParser.ConstraintsClauseContext ctx) {
        repo.put(ctx, repo.get(ctx.mathAssertionExp()));
    }

    @Override
    public void exitRequiresClause(ResolveParser.RequiresClauseContext ctx) {
        repo.put(ctx, repo.get(ctx.mathAssertionExp()));
    }

    @Override
    public void exitEnsuresClause(ResolveParser.EnsuresClauseContext ctx) {
        repo.put(ctx, repo.get(ctx.mathAssertionExp()));
    }

    @Override
    public void exitProgPrimaryExp(ResolveParser.ProgPrimaryExpContext ctx) {
        repo.put(ctx, repo.get(ctx.progPrimary()));
    }

    @Override
    public void exitProgPrimary(ResolveParser.ProgPrimaryContext ctx) {
        repo.put(ctx, repo.get(ctx.getChild(0)));
    }

    @Override
    public void exitProgSelectorExp(ResolveParser.ProgSelectorExpContext ctx) {
        PExp rhs = repo.get(ctx.rhs);
        ProgType t = annotations.progTypes.get(ctx.rhs);
        repo.put(ctx, new PSelector(repo.get(ctx.lhs), rhs));
    }

    @Override
    public void exitProgParamExp(ResolveParser.ProgParamExpContext ctx) {
        PApplyBuilder result = new PApplyBuilder(repo.get(ctx.progSymbolExp()))
                .arguments(Utils.collect(PExp.class, ctx.progExp(), repo))
                .applicationType(getMathClssfctn(ctx));
        repo.put(ctx, result.build());
    }

    @Override
    public void exitProgSymbolExp(ResolveParser.ProgSymbolExpContext ctx) {
        PSymbolBuilder result = new PSymbolBuilder(ctx.name.getText())
                .progType(annotations.progTypes.get(ctx))
                .mathClssfctn(getMathClssfctn(ctx))
                .qualifier(ctx.qualifier);
        repo.put(ctx, result.build());
    }

    @Override
    public void exitProgNestedExp(ResolveParser.ProgNestedExpContext ctx) {
        repo.put(ctx, repo.get(ctx.progExp()));
    }

    @Override
    public void exitProgInfixExp(ResolveParser.ProgInfixExpContext ctx) {
        List<ProgType> argTypes = Utils.apply(ctx.progExp(), annotations.progTypes::get);
        StdTemplateProgOps.BuiltInOpAttributes attr = StdTemplateProgOps.convert(ctx.name.getStart(), argTypes);
        PSymbol operator = new PSymbolBuilder(attr.name.getText())
                .qualifier(attr.qualifier.getText())
                .mathClssfctn(getMathClssfctn(ctx))  //<- this isn't right yet, this will just be the range.
                .progType(annotations.progTypes.get(ctx))
                .build();
        PApplyBuilder result = new PApplyBuilder(operator)
                .arguments(Utils.collect(PExp.class, ctx.progExp(), repo))
                .applicationType(getMathClssfctn(ctx));
        repo.put(ctx, result.build());
    }

    @Override
    public void exitProgIntegerLiteralExp(ResolveParser.ProgIntegerLiteralExpContext ctx) {
        repo.put(ctx, buildLiteral(ctx.getText(), getMathClssfctn(ctx), annotations.progTypes.get(ctx)));
    }

    @Override
    public void exitProgCharacterLiteralExp(ResolveParser.ProgCharacterLiteralExpContext ctx) {
        repo.put(ctx, buildLiteral(ctx.getText(), getMathClssfctn(ctx), annotations.progTypes.get(ctx)));
    }

    @Override
    public void exitProgStringLiteralExp(ResolveParser.ProgStringLiteralExpContext ctx) {
        repo.put(ctx, buildLiteral(ctx.getText(), getMathClssfctn(ctx), annotations.progTypes.get(ctx)));
    }

    @NotNull
    private PExp buildLiteral(@NotNull String literalText,
                              @NotNull MathClssftn type,
                              @Nullable ProgType progType) {
        PSymbolBuilder result =
                new PSymbolBuilder(literalText).mathClssfctn(type)
                        .progType(progType)
                        .literal(true);
        return result.build();
    }

    @NotNull
    private MathClssftn getMathClssfctn(ParseTree t) {
        return annotations.mathClssftns.get(t) == null ? g.INVALID : annotations.mathClssftns.get(t);
    }
}
