package edu.clemson.resolve.vcgen.application;

import edu.clemson.resolve.misc.Utils;
import edu.clemson.resolve.parser.ResolveLexer;
import edu.clemson.resolve.proving.absyn.PApply;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PExpListener;
import edu.clemson.resolve.proving.absyn.PSymbol;
import edu.clemson.resolve.vcgen.model.VCAssertiveBlock;
import edu.clemson.resolve.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;
import edu.clemson.resolve.vcgen.model.AssertiveBlock;
import edu.clemson.resolve.vcgen.model.VCRuleBackedStat;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.Token;
import org.jetbrains.annotations.NotNull;
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
        PApply callExp = (PApply) stat.getStatComponents().get(0);

        PExpSomethingListener something = new PExpSomethingListener(block);
        callExp.accept(something);

       /* PExp finalConfirm = block.finalConfirm.getConfirmExp();
        //finalConfirm = finalConfirm.substitute(something.test);
        FlexibleNameSubstitutingListener l =
                new FlexibleNameSubstitutingListener(
                        finalConfirm, something.test);
        finalConfirm.accept(l);
        finalConfirm = l.getSubstitutedExp();
        BasicBetaReducingListener b =
                new BasicBetaReducingListener(something.test, finalConfirm);
        finalConfirm.accept(b);
        finalConfirm = b.getBetaReducedExp();*/
        //return block.finalConfirm(finalConfirm).snapshot();
        return block.snapshot();
    }

    protected static OperationSymbol getOperation(Scope s, PApply app) {
        PSymbol name = (PSymbol)app.getFunctionPortion();
        Token qualifier = (name.getQualifier() != null) ?
                new CommonToken(ResolveLexer.ID, name.getQualifier()) : null;
        try {
            return s.queryForOne(new OperationQuery(qualifier, name.getName(),
                    Utils.apply(app.getArguments(), PExp::getProgType)));
        }
        catch (NoSuchSymbolException|DuplicateSymbolException e) {
            //shouldn't happen; well, depends on s.
            throw new RuntimeException(e);
        }
    }

    protected static class CallRuleApplyingListener extends PExpListener {
        public Map<PExp, PExp> test = new HashMap<>();
        private final Scope s;
        private final VCAssertiveBlock.VCAssertiveBlockBuilder block;

        public CallRuleApplyingListener(
                VCAssertiveBlock.VCAssertiveBlockBuilder block) {
            this.s = block.scope;
            this.block = block;
        }

        @Override public void endPApply(@NotNull PApply e) {
            PApply thisExp = (PApply)e.substitute(test);
            PSymbol name = (PSymbol)e.getFunctionPortion();
            test.clear(); //TODO: hmmmm..
            List<PExp> actuals = thisExp.getArguments();

            OperationSymbol op = getOperation(s, e);

            List<PExp> formals = Utils.apply(op.getParameters(),
                    ProgParameterSymbol::asPSymbol);
            PExp opRequires = op.getRequires().substitute(formals, actuals);
            opRequires = opRequires.substitute(
                    block.getSpecializationsForFacility(name.getQualifier()));
            block.confirm(opRequires);
        }
    }

    @Override public String getDescription() {
        return "explicit (simple) call rule application";
    }
}

     /*@Override public void endPSymbol(PSymbol e) {
        if (!e.isFunctionApplication()) return;

        PSymbol thisExp = (PSymbol)e.substitute(test);
        test.clear(); //TODO: hmmmm..
        List<PExp> actuals = thisExp.getArguments();

        OperationSymbol op = ExplicitCallApplicationStrategy.getOperation(s, e);
        List<PExp> formals = op.getParameters().stream()
                .map(ProgParameterSymbol::asPSymbol)
                .collect(Collectors.toList());

        PExp opRequires = op.getRequires().substitute(formals, actuals);
        opRequires = opRequires.substitute(
                block.getSpecializationsForFacility(thisExp.getQualifier()));
        block.confirm(opRequires);

        PExp opEnsures = op.getEnsures();

        Iterator<ProgParameterSymbol> formalParamIter =
                op.getParameters().iterator();
        Iterator<PExp> actualParamIter = e.getArguments().iterator();

        Map<PExp, PExp> intermediateBindings = new LinkedHashMap<>();
        Map<PExp, PExp> ensuresEqualities = new HashMap<>();

        for (PExp equals : opEnsures.splitIntoConjuncts()) {
            if (equals.isEquality()) {
                ensuresEqualities.put(equals.getSubExpressions().get(0),
                        equals.getSubExpressions().get(1));
            }
        }
        if (ensuresEqualities.containsKey(e.withArgumentsErased())) {
            intermediateBindings.put(e, ensuresEqualities.get(e.withArgumentsErased()));
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
        for (Map.Entry<PExp, PExp> exp : intermediateBindings.entrySet()) {
            //update our list of formal params to account for incoming-valued refs
            //to themselves in the ensures clause
            List<PExp> varsToReplaceInEnsures = new ArrayList<>(formals);
            for (PSymbol f : exp.getValue().getIncomingVariables(true)) {
                Collections.replaceAll(varsToReplaceInEnsures,
                        f.withIncomingSignsErased(), f);
            }*/

/**
 * Now we substitute the formals for actuals in the rhs of the ensures
 * ({@code f}), THEN replace all occurences of {@code v} in {@code Q}
 * with the modified {@code f}s (formally, {@code Q[v ~> f[x ~> u]]}).
 */
           /* PExp t = exp.getValue();
            FlexibleNameSubstitutingListener l =
                    new FlexibleNameSubstitutingListener(
                            t, varsToReplaceInEnsures, actuals);
            t.accept(l);
            // PExp v = e.getValue().substitute(copyFormals, actuals);
            PExp substitutedExp = l.getSubstitutedExp();
            test.put(exp.getKey(), substitutedExp);
        }
        int i;
        i=0;

    @Override public String getDescription() {
        return "explicit (simple) call rule application";
    }
}
