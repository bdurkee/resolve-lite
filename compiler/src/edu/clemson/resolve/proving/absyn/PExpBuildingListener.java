package edu.clemson.resolve.proving.absyn;

import edu.clemson.resolve.compiler.AnnotatedModule;
import edu.clemson.resolve.misc.Utils;
import edu.clemson.resolve.parser.ResolveParser;
import edu.clemson.resolve.parser.ResolveBaseListener;
import edu.clemson.resolve.proving.absyn.PSymbol.PSymbolBuilder;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rsrg.semantics.*;
import org.rsrg.semantics.programtype.ProgType;

import java.util.*;

/** Converts parse tree math exprs to an equivalent abstract-syntax form,
 *  represented by the {@link PExp} hierarchy. Get the final,
 *  built {@link PExp} via a call to {@link #getBuiltPExp(ParseTree)}.
 */
public class PExpBuildingListener<T extends PExp> extends ResolveBaseListener {

    private final AnnotatedModule annotations;
    private final ParseTreeProperty<PExp> repo;

    private final Map<String, Quantification> quantifiedVars =
            new HashMap<>();
    private final boolean skipDummyQuantifierNodes;
    private final DumbTypeGraph g;

    /** Constructs a new {@code PExpBuildingListener} given an
     *  {@link AnnotatedModule} with it's associated expression tree mappings.
     *
     *  @param g a typegraph
     *  @param annotations annotations to be used for constructing expressions
     */
    public PExpBuildingListener(@NotNull DumbTypeGraph g,
                                @NotNull AnnotatedModule annotations) {
        this(g, annotations, false);
    }

    /** Constructs a new {@code PExpBuildingListener} given an instance of
     *  {@link DumbTypeGraph}, some module {@code annotations} and a boolean flag
     *  {@code skipDummyQuantifiedNodes} indicating whether or not to construct
     *  special, explicit syntactic node representing existential or universal
     *  quantifiers.
     *
     *  @param annotations annotations to be used for constructing expressions
     */
    public PExpBuildingListener(@NotNull DumbTypeGraph g,
                                @NotNull AnnotatedModule annotations,
                                boolean skipDummyQuantifiedNodes) {
        this.g = g;
        this.annotations = annotations;
        this.skipDummyQuantifierNodes = skipDummyQuantifiedNodes;
        this.repo = annotations.mathASTs;
    }

    /** Retrive the final built expr from concrete node {@code t}. */
    @SuppressWarnings("unchecked")
    @Nullable public T getBuiltPExp(ParseTree t) {
        return (T) repo.get(t);
    }

/*    @Override public void exitMathTypeExp(ResolveParser.MathTypeExpContext ctx) {
        repo.put(ctx, repo.get(ctx.mathExp()));
    }

    @Override public void exitMathAssertionExp(
            ResolveParser.MathAssertionExpContext ctx) {
        repo.put(ctx, repo.get(ctx.getChild(0)));
    }

    @Override public void exitMathTypeAssertionExp(
            ResolveParser.MathTypeAssertionExpContext ctx) {
        repo.put(ctx, repo.get(ctx.mathExp()));
    }

    @Override public void exitMathNestedExp(ResolveParser.MathNestedExpContext ctx) {
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

    @Override public void exitMathPrefixAppExp(
            ResolveParser.MathPrefixAppExpContext ctx) {
        List<? extends ParseTree> args = ctx.mathExp()
                .subList(1, ctx.mathExp().size());
        PApplyBuilder result =
                new PApplyBuilder(repo.get(ctx.name))
                        .arguments(Utils.collect(PExp.class, args, repo))
                        .applicationType(getMathType(ctx))
                        .style(PREFIX);
        repo.put(ctx, result.build());
    }

    @Override public void exitMathInfixApplyExp(
            ResolveParser.MathInfixApplyExpContext ctx) {
        PApplyBuilder result =
                new PApplyBuilder((PSymbol) repo.get(ctx.getChild(1)))
                        .applicationType(getMathType(ctx))
                        .style(INFIX)
                        .arguments(Utils.collect(PExp.class, ctx.mathExp(), repo));
        repo.put(ctx, result.build());
    }

    @Override public void exitMathOutfixApplyExp(
            ResolveParser.MathOutfixApplyExpContext ctx) {
        PApplyBuilder result =
                new PApplyBuilder(buildOperatorPSymbol(ctx, ctx.lop, ctx.rop))
                        .applicationType(getMathType(ctx))
                        .applicationTypeValue(getMathTypeValue(ctx))
                        .style(OUTFIX)
                        .arguments(repo.get(ctx.mathExp()));
        repo.put(ctx, result.build());
    }

    @Override public void exitMathMultOp(ResolveParser.MathMultOpContext ctx) {
        repo.put(ctx, buildOperatorPSymbol(ctx, ctx.qualifier, ctx.op));
    }

    @Override public void exitMathAddOp(ResolveParser.MathAddOpContext ctx) {
        repo.put(ctx, buildOperatorPSymbol(ctx, ctx.qualifier, ctx.op));
    }

    @Override public void exitMathRelationalOp(
            ResolveParser.MathRelationalOpContext ctx) {
        repo.put(ctx, buildOperatorPSymbol(ctx, ctx.qualifier, ctx.op));
    }

    @Override public void exitMathBooleanOp(
            ResolveParser.MathBooleanOpContext ctx) {
        repo.put(ctx, buildOperatorPSymbol(ctx, ctx.qualifier, ctx.op));
    }

    @Override public void exitMathEqualityOp(
            ResolveParser.MathEqualityOpContext ctx) {
        repo.put(ctx, buildOperatorPSymbol(ctx, ctx.qualifier, ctx.op));
    }

    @Override public void exitMathImpliesOp(
            ResolveParser.MathImpliesOpContext ctx) {
        repo.put(ctx, buildOperatorPSymbol(ctx, ctx.qualifier, ctx.getStop()));
    }

    @Override public void exitMathSetContainmentOp(
            ResolveParser.MathSetContainmentOpContext ctx) {
        repo.put(ctx, buildOperatorPSymbol(ctx, ctx.qualifier, ctx.op));
    }

    @Override public void exitMathApplicationOp(
            ResolveParser.MathApplicationOpContext ctx) {
        repo.put(ctx, buildOperatorPSymbol(ctx, ctx.qualifier, ctx.op));
    }

    @Override public void exitMathJoiningOp(
            ResolveParser.MathJoiningOpContext ctx) {
        repo.put(ctx, buildOperatorPSymbol(ctx, ctx.qualifier, ctx.op));
    }*/

    private PSymbol buildOperatorPSymbol(@NotNull ParserRuleContext ctx,
                                         @Nullable Token qualifier,
                                         @NotNull Token operator) {
        return new PSymbolBuilder(operator.getText())
                .qualifier(qualifier)
                .mathType(getMathType(ctx))
                .quantification(quantifiedVars.get(operator.getText()))
                .build();
    }

    @Override public void exitMathSymbolExp(
            ResolveParser.MathSymbolExpContext ctx) {
        MathClassification t = getMathType(ctx);
        PSymbolBuilder result =
                new PSymbolBuilder(ctx.name.getText())
                    .qualifier(ctx.qualifier)
                    .incoming(ctx.incoming != null)
                    .quantification(quantifiedVars.get(ctx.name.getText()))
                    .mathType(getMathType(ctx));
        repo.put(ctx, result.build());
    }

    @Override public void exitMathLambdaExp(
            ResolveParser.MathLambdaExpContext ctx) {
        List<PLambda.MathSymbolDeclaration> parameters = new ArrayList<>();
        for (ResolveParser.MathVarDeclGroupContext grp : ctx
                .mathVarDeclGroup()) {
            for (TerminalNode term : grp.ID()) {
                parameters.add(new PLambda.MathSymbolDeclaration(term.getText(),
                        getMathType(grp.mathTypeExp())));
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
                        getMathType(ctx));
        repo.put(ctx, result);
    }

    @Override public void exitMathSetExp(ResolveParser.MathSetExpContext ctx) {
        repo.put(ctx, new PSet(annotations.mathClssftns.get(ctx),
                Utils.collect(PExp.class, ctx.mathExp(), repo)));
    }

    /*@Override public void exitMathSelectorExp(
            ResolveParser.MathSelectorExpContext ctx) {
        repo.put(ctx, new PSelector(repo.get(ctx.lhs), repo.get(ctx.rhs)));
    }*/

    @Override public void exitMathBooleanLiteralExp(
            ResolveParser.MathBooleanLiteralExpContext ctx) {
        PSymbolBuilder result =
                new PSymbolBuilder(ctx.getText())
                        .mathType(getMathType(ctx))
                        .literal(true);
        repo.put(ctx, result.build());
    }

    @Override public void exitMathIntegerLiteralExp(
            ResolveParser.MathIntegerLiteralExpContext ctx) {
        PSymbolBuilder result =
                new PSymbolBuilder(ctx.getText())
                        .mathType(getMathType(ctx)).literal(true);
        repo.put(ctx, result.build());
    }

    /*@Override public void exitConstraintClause(
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

    @Override public void exitProgSelectorExp(
            ResolveParser.ProgSelectorExpContext ctx) {
        PExp rhs = repo.get(ctx.rhs);
        ProgType t = annotations.progTypes.get(ctx.rhs);
        repo.put(ctx, new PSelector(repo.get(ctx.lhs), rhs));
    }

    @Override public void exitProgParamExp(
            ResolveParser.ProgParamExpContext ctx) {
        PApplyBuilder result = new PApplyBuilder(repo.get(ctx.progNamedExp()))
                .arguments(Utils.collect(PExp.class, ctx.progExp(), repo))
                .applicationType(getMathType(ctx));
        repo.put(ctx, result.build());
    }

    @Override public void exitProgNamedExp(
            ResolveParser.ProgNamedExpContext ctx) {
        PSymbolBuilder result = new PSymbolBuilder(ctx.name.getText())
                .progType(annotations.progTypes.get(ctx))
                .progTypeValue(annotations.progTypeValues.get(ctx))
                .mathTypeValue(getMathTypeValue(ctx))
                .mathType(getMathType(ctx))
                .qualifier(ctx.qualifier);
       repo.put(ctx, result.build());
    }

    @Override public void exitProgNestedExp(
            ResolveParser.ProgNestedExpContext ctx) {
        repo.put(ctx, repo.get(ctx.progExp()));
    }

    @Override public void exitProgInfixExp(
            ResolveParser.ProgInfixExpContext ctx) {
        List<ProgType> argTypes = ctx.progExp().stream()
                .map(annotations.progTypes::get).collect(Collectors.toList());
        HardCodedProgOps.BuiltInOpAttributes attr =
                HardCodedProgOps.convert(ctx.op, argTypes);
        PSymbol operator = new PSymbolBuilder(attr.name.getText())
                .qualifier(attr.qualifier.getText())
                .mathType(getMathType(ctx))  //<- this isn't right yet, this will just be the range.
                .progType(annotations.progTypes.get(ctx))
                .progTypeValue(annotations.progTypeValues.get(ctx)).build();
        PApplyBuilder result = new PApplyBuilder(operator)
                .arguments(Utils.collect(PExp.class, ctx.progExp(), repo))
                .applicationType(getMathType(ctx));
        repo.put(ctx, result.build());
    }

    @Override public void exitProgBooleanLiteralExp(
            ResolveParser.ProgBooleanLiteralExpContext ctx) {
        repo.put(ctx, buildLiteral(ctx.getText(), getMathType(ctx),
                getMathTypeValue(ctx), annotations.progTypes.get(ctx)));
    }

    @Override public void exitProgIntegerLiteralExp(
            ResolveParser.ProgIntegerLiteralExpContext ctx) {
        repo.put(ctx, buildLiteral(ctx.getText(), getMathType(ctx),
                getMathTypeValue(ctx), annotations.progTypes.get(ctx)));
    }

    @Override public void exitProgCharacterLiteralExp(
            ResolveParser.ProgCharacterLiteralExpContext ctx) {
        repo.put(ctx, buildLiteral(ctx.getText(), getMathType(ctx),
                getMathTypeValue(ctx), annotations.progTypes.get(ctx)));
    }

    @Override public void exitProgStringLiteralExp(
            ResolveParser.ProgStringLiteralExpContext ctx) {
        repo.put(ctx, buildLiteral(ctx.getText(), getMathType(ctx),
                getMathTypeValue(ctx), annotations.progTypes.get(ctx)));
    }*/

    private PExp buildLiteral(String literalText, MathClassification type, MathClassification typeValue,
                              ProgType progType) {
        PSymbolBuilder result =
                new PSymbolBuilder(literalText).mathType(type)
                        .progType(progType).mathTypeValue(typeValue)
                        .literal(true);
        return result.build();
    }

    private MathClassification getMathType(ParseTree t) {
        return annotations.mathClssftns.get(t) == null ? g.INVALID :
                annotations.mathClssftns.get(t);
    }
}
