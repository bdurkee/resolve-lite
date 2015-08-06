package edu.clemson.resolve.proving.absyn;

import edu.clemson.resolve.compiler.AnnotatedTree;
import edu.clemson.resolve.misc.Utils;
import edu.clemson.resolve.parser.Resolve;
import edu.clemson.resolve.parser.ResolveBaseListener;
import edu.clemson.resolve.proving.absyn.PSymbol.PSymbolBuilder;
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
    private final MTInvalid dummyType;

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

    @Override public void exitMathInfixExp(Resolve.MathInfixExpContext ctx) {
        PSymbolBuilder result =
                new PSymbolBuilder(ctx.op.getText())
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
                last.getArguments()).mathType(last.getMathType());
        repo.put(ctx, result.build());
    }

    @Override public void exitMathFunctionExp(
            Resolve.MathFunctionExpContext ctx) {
        List<PExp> s = Utils.collect(PExp.class, ctx.mathExp(), repo);
        PSymbolBuilder result = new PSymbolBuilder(ctx.name.getText())
                        .arguments(Utils.collect(PExp.class, ctx.mathExp(), repo))
                        .quantification(quantifiedVars.get(ctx.name.getText()))
                        .mathTypeValue(getMathTypeValue(ctx))
                        .mathType(getMathType(ctx));
        repo.put(ctx, result.build());
    }

    @Override public void exitMathSetCollectionExp(
            Resolve.MathSetCollectionExpContext ctx) {
        List<PExp> elements = ctx.mathExp().stream()
                .map(repo::get)
                .collect(Collectors.toList());
        repo.put(ctx, new PSet(getMathType(ctx), getMathTypeValue(ctx), elements));
    }

    @Override public void exitMathBooleanExp(
            Resolve.MathBooleanExpContext ctx) {
        PSymbolBuilder result = new PSymbol.PSymbolBuilder(ctx.getText()) //
                .mathType(getMathType(ctx)).literal(true);
        repo.put(ctx, result.build());
    }

    @Override public void exitMathIntegerExp(
            Resolve.MathIntegerExpContext ctx) {
        PSymbolBuilder result = new PSymbol.PSymbolBuilder(ctx.getText()) //
                .mathType(getMathType(ctx)).literal(true);
        repo.put(ctx, result.build());
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

    @Override public void exitProgInfixExp(Resolve.ProgInfixExpContext ctx) {
        Utils.BuiltInOpAttributes attr = Utils.convertProgramOp(ctx.op);
        PSymbolBuilder result =
                new PSymbolBuilder(attr.name.getText())
                        .arguments(Utils.collect(PExp.class, ctx.progExp(), repo))
                        .qualifier(attr.qualifier)
                        .progType(progTypes.get(ctx)) //
                        .mathTypeValue(getMathTypeValue(ctx)) //
                        .mathType(getMathType(ctx));
        repo.put(ctx, result.build());
    }

    @Override public void exitProgPrimaryExp(Resolve.ProgPrimaryExpContext ctx) {
        repo.put(ctx, repo.get(ctx.progPrimary()));
    }

    @Override public void exitProgPrimary(Resolve.ProgPrimaryContext ctx) {
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

    @Override public void exitProgMemberExp(Resolve.ProgMemberExpContext ctx) {
        List<String> nameComponents = new ArrayList<>();
        nameComponents.add(ctx.progNamedExp().getText());
        nameComponents.addAll(ctx.ID().stream()
                .map(TerminalNode::getText).collect(Collectors.toList()));
        repo.put(ctx, new PSymbolBuilder(Utils.join(nameComponents, "."))
                .mathType(types.get(ctx)).build());
    }

    @Override public void exitProgIntegerExp(Resolve.ProgIntegerExpContext ctx) {
        repo.put(ctx, buildLiteral(ctx.getText(), types.get(ctx),
                typeValues.get(ctx), progTypes.get(ctx)));
    }

    @Override public void exitProgCharacterExp(
            Resolve.ProgCharacterExpContext ctx) {
        repo.put(ctx, buildLiteral(ctx.getText(), types.get(ctx),
                typeValues.get(ctx), progTypes.get(ctx)));
    }

    @Override public void exitProgStringExp(Resolve.ProgStringExpContext ctx) {
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
