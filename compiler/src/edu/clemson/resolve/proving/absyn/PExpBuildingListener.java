package edu.clemson.resolve.proving.absyn;

import edu.clemson.resolve.compiler.AnnotatedTree;
import edu.clemson.resolve.misc.HardCodedProgOps;
import edu.clemson.resolve.misc.Utils;
import edu.clemson.resolve.parser.Resolve;
import edu.clemson.resolve.parser.ResolveBaseListener;
import edu.clemson.resolve.parser.ResolveParser;
import edu.clemson.resolve.proving.absyn.PSymbol.PSymbolBuilder;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.rsrg.semantics.MTInvalid;
import org.rsrg.semantics.MTType;
import org.rsrg.semantics.Quantification;
import org.rsrg.semantics.programtype.PTType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Converts parse tree math exprs to an equivalent abstract-syntax form,
 * represented by the {@link PExp} hierarchy.
 */
public class PExpBuildingListener<T extends PExp> extends ResolveBaseListener {

    private final ParseTreeProperty<MTType> types, typeValues;
    private final ParseTreeProperty<PTType> progTypes;
    private final ParseTreeProperty<PExp> repo;

    private final Map<String, Quantification> quantifiedVars = new HashMap<>();
    private final MTInvalid dummyType;

    public PExpBuildingListener(AnnotatedTree annotations) {
        this(annotations, null);
    }

    public PExpBuildingListener(AnnotatedTree annotations, MTInvalid dummyType) {
        this.types = annotations.mathTypes;
        this.typeValues = annotations.mathTypeValues;
        this.progTypes = annotations.progTypes;
        this.repo = annotations.mathPExps;
        this.dummyType = dummyType;
    }

    @SuppressWarnings("unchecked") public T getBuiltPExp(ParseTree t) {
        return (T) repo.get(t);
    }

    @Override public void exitCorrespondenceClause(
            Resolve.CorrespondenceClauseContext ctx) {
        repo.put(ctx, repo.get(ctx.mathAssertionExp()));
    }

    @Override public void exitConstraintClause(
            Resolve.ConstraintClauseContext ctx) {
        repo.put(ctx, repo.get(ctx.mathAssertionExp()));
    }

    @Override public void exitConventionClause(
            Resolve.ConventionClauseContext ctx) {
        repo.put(ctx, repo.get(ctx.mathAssertionExp()));
    }

    @Override public void exitRequiresClause(Resolve.RequiresClauseContext ctx) {
        repo.put(ctx, repo.get(ctx.mathAssertionExp()));
    }

    @Override public void exitEnsuresClause(Resolve.EnsuresClauseContext ctx) {
        repo.put(ctx, repo.get(ctx.mathAssertionExp()));
    }

    @Override public void exitMathTypeAssertionExp(
            Resolve.MathTypeAssertionExpContext ctx) {
        repo.put(ctx, repo.get(ctx.mathExp()));
    }

    @Override public void exitMathTypeExp(Resolve.MathTypeExpContext ctx) {
        repo.put(ctx, repo.get(ctx.mathExp()));
    }

    @Override public void exitMathAssertionExp(
            Resolve.MathAssertionExpContext ctx) {
        repo.put(ctx, repo.get(ctx.getChild(0)));
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
                    quantifier.equals("Forall") ? Quantification.UNIVERSAL
                            : Quantification.EXISTENTIAL);
        }
    }

    @Override public void exitMathQuantifiedExp(
            Resolve.MathQuantifiedExpContext ctx) {
        for (TerminalNode term : ctx.mathVariableDeclGroup().ID()) {
            quantifiedVars.remove(term.getText());
        }
        repo.put(ctx, repo.get(ctx.mathAssertionExp()));
    }

    @Override public void exitMathUnaryExp(Resolve.MathUnaryExpContext ctx) {
        PSymbolBuilder result =
                new PSymbolBuilder(ctx.op.getText())
                        .arguments(repo.get(ctx.mathExp()))
                        .style(PSymbol.DisplayStyle.PREFIX)
                        .mathTypeValue(getMathTypeValue(ctx))
                        .mathType(getMathType(ctx));
        repo.put(ctx, result.build());
    }

    @Override public void exitMathInfixExp(Resolve.MathInfixExpContext ctx) {
        //System.out.println("mathInfixExp="+ctx.getText());
        PSymbolBuilder result =
                new PSymbolBuilder(ctx.op.getText())
                        .arguments(Utils.collect(PExp.class, ctx.mathExp(), repo))
                        .style(PSymbol.DisplayStyle.INFIX)
                        .mathTypeValue(getMathTypeValue(ctx))
                        .mathType(getMathType(ctx));
        repo.put(ctx, result.build());
    }

    @Override public void exitMathCustomInfixExp(Resolve.MathCustomInfixExpContext ctx) {
        PSymbolBuilder result =
                new PSymbolBuilder(ctx.mathSymbol().getText())
                        .arguments(Utils.collect(PExp.class, ctx.mathExp(), repo))
                        .style(PSymbol.DisplayStyle.INFIX)
                        .mathTypeValue(getMathTypeValue(ctx))
                        .mathType(getMathType(ctx));
        repo.put(ctx, result.build());
    }

    @Override public void exitMathOutfixExp(Resolve.MathOutfixExpContext ctx) {
        PSymbolBuilder result =
                new PSymbolBuilder(ctx.lop.getText(), ctx.rop.getText()) //
                        .arguments(repo.get(ctx.mathExp())) //
                        .style(PSymbol.DisplayStyle.OUTFIX) //
                        .mathTypeValue(getMathTypeValue(ctx)) //
                        .mathType(getMathType(ctx));
        repo.put(ctx, result.build());
    }

    @Override public void exitMathVariableExp(
            Resolve.MathVariableExpContext ctx) {
        PSymbolBuilder result = new PSymbolBuilder(ctx.name.getText())
                        .qualifier(ctx.qualifier)
                        .incoming(ctx.AT() != null)
                        .quantification(quantifiedVars.get(ctx.name.getText()))
                        .mathTypeValue(getMathTypeValue(ctx))
                        .mathType(getMathType(ctx));
        repo.put(ctx, result.build());
    }

    @Override public void exitMathLambdaExp(Resolve.MathLambdaExpContext ctx) {
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

    @Override public void exitMathSegmentsExp(
            Resolve.MathSegmentsExpContext ctx) {
        //Todo: Type the individual segs of a seg exp.
        List<String> nameComponents = ctx.mathFunctionApplicationExp().stream()
                .map(app -> ((PSymbol) repo.get(app)).getName())
                .collect(Collectors.toList());
        PSymbol last = (PSymbol)repo.get(ctx.mathFunctionApplicationExp()
                .get(ctx.mathFunctionApplicationExp().size() - 1));

        String name = Utils.join(nameComponents, ".");
        PSymbolBuilder result = new PSymbolBuilder(name).arguments(
                last.getArguments()).incoming(ctx.AT() != null)
                .mathType(last.getMathType());

        repo.put(ctx, result.build());
    }

    @Override public void exitMathFunctionRestrictionExp(
            ResolveParser.MathFunctionRestrictionExpContext ctx) {
        PSymbolBuilder result = new PSymbolBuilder("App_Op")
                .arguments(repo.get(ctx.restrictionFunctionExp()), repo.get(ctx.mathExp()))
                //.quantification(quantifiedVars.get(ctx.name.getText()))
                .mathTypeValue(getMathTypeValue(ctx))
                .mathType(getMathType(ctx));
        repo.put(ctx, result.build());
    }

    @Override public void exitRestrictionFunctionExp(
            Resolve.RestrictionFunctionExpContext ctx) {
        repo.put(ctx, repo.get(ctx.getChild(0)));
    }

    @Override public void exitMathFunctionExp(
            Resolve.MathFunctionExpContext ctx) {
        List<PExp> s = Utils.collect(PExp.class, ctx.mathExp(), repo);
        PSymbolBuilder result = new PSymbolBuilder(ctx.name.getText())
                        .arguments(Utils.collect(PExp.class, ctx.mathExp(), repo))
                        .quantification(quantifiedVars.get(ctx.name.getText()))
                        .incoming(ctx.getText().startsWith("@"))
                        .mathTypeValue(getMathTypeValue(ctx))
                        .mathType(getMathType(ctx));
        repo.put(ctx, result.build());
    }

    @Override public void exitMathSetCollectionExp(
            ResolveParser.MathSetCollectionExpContext ctx) {
        List<PExp> elements = ctx.mathExp().stream()
                .map(repo::get)
                .collect(Collectors.toList());
        repo.put(ctx, new PSet(getMathType(ctx), getMathTypeValue(ctx), elements));
    }

    @Override public void exitMathBooleanLiteralExp(
            Resolve.MathBooleanLiteralExpContext ctx) {
        PSymbolBuilder result = new PSymbol.PSymbolBuilder(ctx.getText()) //
                .mathType(getMathType(ctx)).literal(true);
        repo.put(ctx, result.build());
    }

    @Override public void exitMathIntegerLiteralExp(
            Resolve.MathIntegerLiteralExpContext ctx) {
        PSymbolBuilder result = new PSymbol.PSymbolBuilder(ctx.getText()) //
                .mathType(getMathType(ctx)).literal(true);
        repo.put(ctx, result.build());
    }

    @Override public void exitModuleArgument(Resolve.ModuleArgumentContext ctx) {
        repo.put(ctx, repo.get(ctx.progExp()));
    }

    @Override public void exitProgParamExp(Resolve.ProgParamExpContext ctx) {
        PSymbolBuilder result =
                new PSymbolBuilder(ctx.name.getText())
                        .arguments(
                                Utils.collect(PExp.class, ctx.progExp(), repo))
                        .progType(progTypes.get(ctx)).qualifier(ctx.qualifier)
                        .mathTypeValue(getMathTypeValue(ctx))
                        .mathType(getMathType(ctx));
        repo.put(ctx, result.build());
    }

    @Override public void exitProgUnaryExp(Resolve.ProgUnaryExpContext ctx) {
        repo.put(ctx, buildSugaredProgExp(ctx, ctx.op, ctx.progExp()));
    }

    @Override public void exitProgInfixExp(Resolve.ProgInfixExpContext ctx) {
        repo.put(ctx, buildSugaredProgExp(ctx, ctx.op, ctx.progExp()));
    }

    @Override public void exitProgPostfixExp(Resolve.ProgPostfixExpContext ctx) {
        repo.put(ctx, buildSugaredProgExp(ctx, ctx.op, ctx.progExp()));
    }

    private PExp buildSugaredProgExp(ParserRuleContext ctx,
                                     Token opName, ParserRuleContext ... args) {
        return buildSugaredProgExp(ctx, opName, Arrays.asList(args));
    }

    private PExp buildSugaredProgExp(ParserRuleContext ctx,
                                     Token op,
                                     List<? extends ParserRuleContext> args) {
        List<PTType> argTypes = args.stream().map(progTypes::get)
                .collect(Collectors.toList());
        HardCodedProgOps.BuiltInOpAttributes o =
                HardCodedProgOps.convert(op, argTypes);
        return new PSymbolBuilder(o.name.getText())
                        .arguments(Utils.collect(PExp.class, args, repo))
                        .qualifier(o.qualifier)
                        .progType(progTypes.get(ctx)) //
                        .mathTypeValue(getMathTypeValue(ctx)) //
                        .mathType(getMathType(ctx)).build();
    }

    @Override public void exitProgPrimaryExp(Resolve.ProgPrimaryExpContext ctx) {
        repo.put(ctx, repo.get(ctx.progPrimary()));
    }

    @Override public void exitProgPrimary(Resolve.ProgPrimaryContext ctx) {
        repo.put(ctx, repo.get(ctx.getChild(0)));
    }

    @Override public void exitProgVarExp(Resolve.ProgVarExpContext ctx) {
        repo.put(ctx, repo.get(ctx.getChild(0)));
    }

    @Override public void exitProgNamedExp(Resolve.ProgNamedExpContext ctx) {
        PSymbolBuilder result = new PSymbolBuilder(ctx.name.getText()) //
                .mathTypeValue(getMathTypeValue(ctx)) //
                .progType(progTypes.get(ctx)).qualifier(ctx.qualifier) //
                .mathType(getMathType(ctx));
        //System.out.println("progNamedExp="+ctx.getText());
        repo.put(ctx, result.build());
    }

    @Override public void exitProgNestedExp(Resolve.ProgNestedExpContext ctx) {
        repo.put(ctx, repo.get(ctx.progExp()));
    }

    @Override public void exitProgMemberExp(Resolve.ProgMemberExpContext ctx) {
        List<String> nameComponents = new ArrayList<>();
        nameComponents.add(ctx.progNamedExp().getText());
        nameComponents.addAll(ctx.ID().stream()
                .map(TerminalNode::getText).collect(Collectors.toList()));
        repo.put(ctx, new PSymbolBuilder(Utils.join(nameComponents, "."))
                .mathType(types.get(ctx)).progType(progTypes.get(ctx)).build());
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
            ResolveParser.ProgCharacterLiteralExpContext ctx) {
        repo.put(ctx, buildLiteral(ctx.getText(), types.get(ctx),
                typeValues.get(ctx), progTypes.get(ctx)));
    }

    @Override public void exitProgStringLiteralExp(
            Resolve.ProgStringLiteralExpContext ctx) {
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

    private MTType getMathType(ParseTree t) {
        return types.get(t) == null ? dummyType : types.get(t);
    }

    private MTType getMathTypeValue(ParseTree t) {
        return typeValues.get(t) == null ? dummyType : typeValues.get(t);
    }
}
