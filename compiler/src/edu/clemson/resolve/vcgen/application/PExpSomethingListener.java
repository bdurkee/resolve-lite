package edu.clemson.resolve.vcgen.application;

import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PExpListener;
import edu.clemson.resolve.proving.absyn.PSymbol;
import edu.clemson.resolve.vcgen.FlexibleNameSubstitutingListener;
import edu.clemson.resolve.vcgen.application.ExplicitCallApplicationStrategy;
import edu.clemson.resolve.vcgen.model.VCAssertiveBlock;
import org.rsrg.semantics.Scope;
import org.rsrg.semantics.symbol.OperationSymbol;
import org.rsrg.semantics.symbol.ProgParameterSymbol;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by daniel on 9/28/15.
 */
public class PExpSomethingListener extends PExpListener {

    public Map<PExp, PExp> test = new HashMap<>();
    private final Scope s;
    private final VCAssertiveBlock.VCAssertiveBlockBuilder block;

    public PExpSomethingListener(
            VCAssertiveBlock.VCAssertiveBlockBuilder block) {
        this.s = block.scope;
        this.block = block;
    }

    @Override public void endPSymbol(PSymbol e) {
        if (!e.isFunctionApplication()) return;

        PSymbol thisExp = (PSymbol)e.substitute(test);
        test.clear(); //TODO: hmmmm..
        List<PExp> actuals = thisExp.getArguments();

        OperationSymbol op = ExplicitCallApplicationStrategy.getOperation(s, e);
        List<PExp> formals = op.getParameters().stream()
                .map(ProgParameterSymbol::asPSymbol)
                .collect(Collectors.toList());

        PExp opRequires = block.getPExpFor(op.getRequires());
        opRequires = opRequires.substitute(formals, actuals);
        block.confirm(opRequires);

        PExp opEnsures = block.getPExpFor(op.getEnsures());
        /*opEnsures = opEnsures.substitute(
                ModelBuilderProto.getFacilitySpecializations(
                        block.symtab.mathPExps,
                        block.scope, call.getQualifier()));*/ //Todo: Hmmm. not sure about this one

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
            for (PSymbol f : exp.getValue().getIncomingSymbols(true)) {
                Collections.replaceAll(varsToReplaceInEnsures,
                        f.withIncomingSignsErased(), f);
            }

            /**
             * Now we substitute the formals for actuals in the rhs of the ensures
             * ({@code f}), THEN replace all occurences of {@code v} in {@code Q}
             * with the modified {@code f}s (formally, {@code Q[v ~> f[x ~> u]]}).
             */
            PExp t = exp.getValue();
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
    }
}
