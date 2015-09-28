package edu.clemson.resolve.vcgen.application;

import edu.clemson.resolve.compiler.AnnotatedTree;
import edu.clemson.resolve.parser.ResolveLexer;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PSymbol;
import edu.clemson.resolve.vcgen.BasicBetaReducingListener;
import edu.clemson.resolve.vcgen.FlexibleNameSubstitutingListener;
import edu.clemson.resolve.vcgen.ModelBuilderProto;
import edu.clemson.resolve.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;
import edu.clemson.resolve.vcgen.model.AssertiveBlock;
import edu.clemson.resolve.vcgen.model.VCRuleBackedStat;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.rsrg.semantics.DuplicateSymbolException;
import org.rsrg.semantics.NoSuchSymbolException;
import org.rsrg.semantics.Scope;
import org.rsrg.semantics.programtype.PTType;
import org.rsrg.semantics.query.OperationQuery;
import org.rsrg.semantics.symbol.OperationSymbol;
import org.rsrg.semantics.symbol.ProgParameterSymbol;

import java.util.*;
import java.util.stream.Collectors;

public class ExplicitCallApplicationStrategy
        implements
            StatRuleApplicationStrategy<VCRuleBackedStat> {

    @Override public AssertiveBlock applyRule(
            VCAssertiveBlockBuilder block, VCRuleBackedStat stat) {
        PSymbol callExp = (PSymbol) stat.getStatComponents().get(0);
        OperationSymbol op = getOperation(block.scope, callExp);
        PExp finalConfirm = block.finalConfirm.getConfirmExp();

        Map<PExp, PExp> ensuresReplacements =
                getEnsuresReplacementBindings(op, block, callExp);
        FlexibleNameSubstitutingListener l =
                new FlexibleNameSubstitutingListener(
                        finalConfirm, ensuresReplacements);
        finalConfirm.accept(l);
        finalConfirm = l.getSubstitutedExp();

        //block.finalConfirm(block.finalConfirm.getConfirmExp()
        //        .substitute(replacementActuals, modifiedEnsures));
        BasicBetaReducingListener b =
                new BasicBetaReducingListener(ensuresReplacements, finalConfirm);
        finalConfirm.accept(b);
        finalConfirm = b.getBetaReducedExp();

        //TODO: We're going to mutate the repo in this assertive builder block
        //to refer to the ensures clause, not the actual call...
        if (ensuresReplacements.containsKey(callExp.withArgumentsErased())) {
            block.argInstantiations.put(stat.getStatComponents().get(0),
                    ensuresReplacements.get(callExp.withArgumentsErased()));
        }
        //block.repo.put(stat.getDefiningContext(), )
        return block.finalConfirm(finalConfirm).snapshot();
    }

    /**
     * In the explicit call rule, this helper method simply returns the
     * result of the {@code f[x ~> u]} part of the overall
     * step: {@code Q[v ~> f[x ~> u]]}.
     */
    private Map<PExp, PExp> getEnsuresReplacementBindings(
            OperationSymbol op, VCAssertiveBlockBuilder block, PSymbol call) {

        List<PExp> actuals = new ArrayList<>();
        for (PExp arg : call.getArguments()) {
            if (block.argInstantiations.containsKey(arg)) {
                actuals.add(block.argInstantiations.get(arg));
            }
            else {
                actuals.add(arg);
            }
        }
        List<PExp> formals = op.getParameters().stream()
                .map(ProgParameterSymbol::asPSymbol).collect(Collectors.toList());

        PExp opRequires = block.getPExpFor(op.getRequires());

        /*opRequires = opRequires.substitute(
                ModelBuilderProto.getFacilitySpecializations(
                        block.repo,
                        block.scope, call.getQualifier()));*/
        opRequires = opRequires.substitute(formals, actuals);
        block.confirm(opRequires);

        PExp opEnsures = block.getPExpFor(op.getEnsures());
        /*opEnsures = opEnsures.substitute(
                ModelBuilderProto.getFacilitySpecializations(
                        block.symtab.mathPExps,
                        block.scope, call.getQualifier()));*/ //Todo: Hmmm. not sure about this one

        Iterator<ProgParameterSymbol> formalParamIter =
                op.getParameters().iterator();
        Iterator<PExp> actualParamIter = call.getArguments().iterator();

        Map<PExp, PExp> intermediateBindings = new LinkedHashMap<>();
        Map<PExp, PExp> ensuresEqualities = new HashMap<>();

        for (PExp equals : opEnsures.splitIntoConjuncts()) {
            if (equals.isEquality()) {
                ensuresEqualities.put(equals.getSubExpressions().get(0),
                        equals.getSubExpressions().get(1));
            }
        }
        if (ensuresEqualities.containsKey(call.withArgumentsErased())) {
            intermediateBindings.put(call.withArgumentsErased(),
                    ensuresEqualities.get(call.withArgumentsErased()));
        }

        while (formalParamIter.hasNext()) {
            ProgParameterSymbol formal = formalParamIter.next();
            PExp actual = actualParamIter.next();
            if (formal.getMode() == ProgParameterSymbol.ParameterMode.UPDATES) {
                if (!ensuresEqualities.containsKey(formal.asPSymbol())) {
                    continue;
                }
                intermediateBindings.put(actual,
                        ensuresEqualities.get(formal.asPSymbol()));
            }
        }
        Map<PExp, PExp> resultBindings = new LinkedHashMap<>();
        for (Map.Entry<PExp, PExp> e : intermediateBindings.entrySet()) {
            //update our list of formal params to account for incoming-valued refs
            //to themselves in the ensures clause
            List<PExp> varsToReplaceInEnsures = new ArrayList<>(formals);
            for (PSymbol f : e.getValue().getIncomingSymbols(true)) {
                Collections.replaceAll(varsToReplaceInEnsures,
                        f.withIncomingSignsErased(), f);
            }

            /**
             * Now we substitute the formals for actuals in the rhs of the ensures
             * ({@code f}), THEN replace all occurences of {@code v} in {@code Q}
             * with the modified {@code f}s (formally, {@code Q[v ~> f[x ~> u]]}).
             */
            PExp t = e.getValue();
            FlexibleNameSubstitutingListener l =
                    new FlexibleNameSubstitutingListener(
                            t, varsToReplaceInEnsures, actuals);
            t.accept(l);
           // PExp v = e.getValue().substitute(copyFormals, actuals);
            PExp substitutedExp = l.getSubstitutedExp();
            resultBindings.put(e.getKey(), substitutedExp);
        }
        return resultBindings;
    }

    protected static OperationSymbol getOperation(Scope s, PSymbol app) {
        List<PTType> argTypes = app.getArguments().stream()
                .map(PExp::getProgType).collect(Collectors.toList());
        try {
            return s.queryForOne(new OperationQuery(
                    (app.getQualifier() != null) ?
                            new CommonToken(ResolveLexer.ID, app.getQualifier()) : null,
                        app.getName(), argTypes));
        }
        catch (NoSuchSymbolException|DuplicateSymbolException e) {
            //shouldn't happen. Well, depends on s.
            throw new RuntimeException(e);
        }
    }

    @Override public String getDescription() {
        return "explicit (simple) call rule application";
    }
}
