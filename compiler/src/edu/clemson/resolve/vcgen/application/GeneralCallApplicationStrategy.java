package edu.clemson.resolve.vcgen.application;

import edu.clemson.resolve.misc.Utils;
import edu.clemson.resolve.proving.absyn.PApply;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PExpListener;
import edu.clemson.resolve.proving.absyn.PSymbol;
import edu.clemson.resolve.proving.absyn.PSymbol.PSymbolBuilder;
import edu.clemson.resolve.vcgen.model.AssertiveBlock;
import edu.clemson.resolve.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;
import edu.clemson.resolve.vcgen.model.VCRuleBackedStat;
import org.jetbrains.annotations.NotNull;
import org.rsrg.semantics.programtype.PTFamily;
import org.rsrg.semantics.symbol.OperationSymbol;
import org.rsrg.semantics.symbol.ProgParameterSymbol;
import org.rsrg.semantics.symbol.ProgParameterSymbol.ParameterMode;

import java.util.*;
import java.util.stream.Collectors;

import static edu.clemson.resolve.vcgen.application.ExplicitCallApplicationStrategy.*;
import static org.rsrg.semantics.symbol.ProgParameterSymbol.ParameterMode.*;

public class GeneralCallApplicationStrategy
        implements
            StatRuleApplicationStrategy<VCRuleBackedStat> {

    @Override public AssertiveBlock applyRule(VCAssertiveBlockBuilder block,
                                              VCRuleBackedStat stat) {
        PApply callExp = (PApply) stat.getStatComponents().get(0);
        GeneralCallRuleSubstitutor applier =
                new GeneralCallRuleSubstitutor(block);
        callExp.accept(applier);

        return block.finalConfirm(applier.getCompletedExp())
                .snapshot();
    }

    //TODO: Walk through this step by step in a .md file. Then store the .md file in docs/
    public static class GeneralCallRuleSubstitutor extends PExpListener {
        private final VCAssertiveBlockBuilder block;
        public Map<PExp, PExp> test = new HashMap<>();

        public GeneralCallRuleSubstitutor(VCAssertiveBlockBuilder block) {
            this.block = block;
        }

        public PExp getCompletedExp() {
            return block.finalConfirm.getConfirmExp().substitute(test);
        }

        @Override public void endPApply(@NotNull PApply e) {
            OperationSymbol op = getOperation(block.scope, e);
            final Set<ParameterMode> distinguishedModes =
                    new HashSet<>(Arrays.asList(UPDATES, REPLACES, ALTERS, CLEARS));
            PExp newAssume = op.getEnsures();
            List<PExp> formalExps = Utils.apply(op.getParameters(),
                    ProgParameterSymbol::asPSymbol);
            block.confirm(op.getRequires().substitute(formalExps, e.getArguments()));
            for (ProgParameterSymbol p : op.getParameters()) {
                //T1.Constraint(t) /\ T3.Constraint(v) /\ T6.Constraint(y) /\
                //postcondition
                //TODO: Ask about these constraints
               if (distinguishedModes.contains(p.getMode())) {
                    if (p.getDeclaredType() instanceof PTFamily) {
                        newAssume = block.g.formConjunct(newAssume,
                                ((PTFamily) p.getDeclaredType())
                                        .getConstraint());
                    }
                }
            }
            PExp RP = block.finalConfirm.getConfirmExp();
            Map<PExp, PExp> newAssumeSubtitutions = new HashMap<>();
            Iterator<ProgParameterSymbol> formalIter =
                    op.getParameters().iterator();
            Iterator<PExp> argIter = e.getArguments().iterator();

            while (formalIter.hasNext()) {
                ProgParameterSymbol curFormal = formalIter.next();
                PExp curActual = (PSymbol)argIter.next();

                //t ~> NQV(RP, a), @t ~> a
                if (curFormal.getMode() == UPDATES) {
                    newAssumeSubtitutions.put(curFormal.asPSymbol(),
                            NQV(RP, (PSymbol) curActual));
                    newAssumeSubtitutions.put(new PSymbolBuilder(curFormal
                                    .asPSymbol()).incoming(true).build(),
                            (PSymbol) curActual);
                }
                //v ~> NQV(RP, b)
                else if (curFormal.getMode() == REPLACES) {
                    newAssumeSubtitutions.put(curFormal.asPSymbol(),
                            NQV(RP, (PSymbol) curActual));
                }
                //@y ~> e, @z ~> f
                else if (curFormal.getMode() == ALTERS ||
                        curFormal.getMode() == CLEARS) {
                    newAssumeSubtitutions.put(
                            new PSymbolBuilder(curFormal.asPSymbol())
                                    .incoming(true).build(), curActual);
                }
                else {
                    newAssumeSubtitutions.put(curFormal.asPSymbol(), curActual);
                }
            }
            //Assume (T1.Constraint(t) /\ T3.Constraint(v) /\ T6.Constraint(y) /\
            //Post [ t ~> NQV(RP, a), @t ~> a, u ~> Math(exp), v ~> NQV(RP, b),
            //       w ~> c, x ~> d, @y ~> e, @z ~> f]
            block.assume(newAssume.substitute(newAssumeSubtitutions));

            //Ok, so this happens down here since the rule is laid out s.t.
            //substitutions occur prior to conjuncting this -- consult the
            //rule and see for yourself
            for (ProgParameterSymbol p : op.getParameters()) {
                //T7.Is_Initial(NQV(RP, f));
                //TODO: See todo above
               /* if (p.getMode() == CLEARS) {
                    PExp initPred =
                            block.g.formInitializationPredicate(
                                    p.getDeclaredType(), p.getName());
                    newAssume = block.g.formConjunct(newAssume, initPred);
                }*/
            }

            //reset the formal param iter in preperation for building the
            //substitution mapping for our confirm
            formalIter = op.getParameters().iterator();
            argIter = e.getArguments().iterator();
            Map<PExp, PExp> confirmSubstitutions = new HashMap<>();
            for (PExp actualArg : e.getArguments()) {
                ProgParameterSymbol curFormal = formalIter.next();
                if (distinguishedModes.contains(curFormal.getMode())) {
                    confirmSubstitutions.put(actualArg,
                            NQV(RP, (PSymbol)actualArg));
                }
            }
            block.finalConfirm(RP.substitute(confirmSubstitutions));
        }
    }

    /** "Next Question-mark Variable" */
    public static PSymbol NQV(PExp RP, PSymbol oldSym) {
        // Add an extra question mark to the front of oldSym
        PSymbol newOldSym = new PSymbolBuilder(oldSym, "?"+oldSym.getName())
                .build();

        // Applies the question mark to oldVar if it is our first time visiting.
        if (RP.containsName(oldSym.getName())) {
            return NQV(RP, newOldSym);
        }
        // Don't need to apply the question mark here.
        else if (RP.containsName(newOldSym.getName())) {
            return NQV(RP, newOldSym);
        }
        else {
            // Return the new variable expression with the question mark
            if (oldSym.getName().charAt(0) != '?') {
                return newOldSym;
            }
        }
        return oldSym;
    }

    @Override public String getDescription() {
        return "general call rule application";
    }
}
