package edu.clemson.resolve.vcgen.application;

import edu.clemson.resolve.misc.Utils;
import edu.clemson.resolve.proving.absyn.PApply;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PExpListener;
import edu.clemson.resolve.proving.absyn.PSymbol;
import edu.clemson.resolve.proving.absyn.PSymbol.PSymbolBuilder;
import edu.clemson.resolve.semantics.symbol.OperationSymbol;
import edu.clemson.resolve.semantics.symbol.ProgParameterSymbol;
import edu.clemson.resolve.semantics.symbol.ProgParameterSymbol.ParameterMode;
import edu.clemson.resolve.vcgen.model.AssertiveBlock;
import edu.clemson.resolve.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;
import edu.clemson.resolve.vcgen.model.VCCall;
import edu.clemson.resolve.vcgen.model.VCRuleBackedStat;
import org.antlr.v4.runtime.ParserRuleContext;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class GeneralCallApplicationStrategy implements VCStatRuleApplicationStrategy<VCCall> {

    @NotNull
    @Override
    public AssertiveBlock applyRule(@NotNull VCAssertiveBlockBuilder block, @NotNull VCCall stat) {
        PApply callExp = (PApply) stat.getStatComponents().get(0);
        GeneralCallRuleSubstitutor applier = new GeneralCallRuleSubstitutor(stat.getDefiningContext(), block);
        callExp.accept(applier);
        return block.finalConfirm(applier.getCompletedExp()).snapshot();
    }

    //TODO: Walk through this step by step in a .md file. Then store the .md file in doc/
    public static class GeneralCallRuleSubstitutor extends PExpListener {
        private final VCAssertiveBlockBuilder block;
        public Map<PExp, PExp> returnEnsuresArgSubstitutions = new HashMap<>();

        private final ParserRuleContext ctx;

        public GeneralCallRuleSubstitutor(@NotNull ParserRuleContext ctx, @NotNull VCAssertiveBlockBuilder block) {
            this.block = block;
            this.ctx = ctx;
        }

        @NotNull
        public PExp getCompletedExp() {
            return block.finalConfirm.getConfirmExp();
        }

        @Override
        public void endPApply(@NotNull PApply e) {
            OperationSymbol op = ExplicitCallApplicationStrategy.getOperation(block.scope, e);
            final Set<ParameterMode> distinguishedModes =
                    new HashSet<>(Arrays.asList(ParameterMode.UPDATES, ParameterMode.REPLACES,
                            ParameterMode.ALTERS, ParameterMode.CLEARS));

            PSymbol functionName = (PSymbol) e.getFunctionPortion();

            PExp newAssume = op.getEnsures();
            List<PExp> formalExps = Utils.apply(op.getParameters(), ProgParameterSymbol::asPSymbol);
            PExp confirmPrecondition = op.getRequires();

            confirmPrecondition = confirmPrecondition
                    .substitute(formalExps, e.getArguments())
                    .substitute(returnEnsuresArgSubstitutions)
                    .withVCInfo(ctx.getStart(), "Requires clause of " + functionName.getName());
            block.confirm(ctx, confirmPrecondition);
            //^^^^^ Here's the old one:
            //block.confirm(ctx, op.getRequires().substitute(formalExps, e.getArguments()));
            /*for (ProgParameterSymbol p : op.getParameters()) {
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
            }*/
            PExp RP = block.finalConfirm.getConfirmExp();
            Map<PExp, PExp> newAssumeSubtitutions = new HashMap<>();
            Iterator<ProgParameterSymbol> formalIter = op.getParameters().iterator();
            Iterator<PExp> argIter = e.getArguments().iterator();

            while (formalIter.hasNext()) {
                ProgParameterSymbol curFormal = formalIter.next();
                PExp curActual = (PExp) argIter.next();

                //t ~> NQV(RP, a), @t ~> a
                if (curFormal.getMode() == ParameterMode.UPDATES) {
                    newAssumeSubtitutions.put(curFormal.asPSymbol(), NQV(RP, (PSymbol) curActual));
                    newAssumeSubtitutions.put(new PSymbolBuilder(
                            curFormal.asPSymbol()).incoming(true).build(), (PSymbol) curActual);
                }
                //v ~> NQV(RP, b)
                else if (curFormal.getMode() == ParameterMode.REPLACES) {
                    newAssumeSubtitutions.put(curFormal.asPSymbol(), NQV(RP, (PSymbol) curActual));
                }
                //@y ~> e, @z ~> f
                else if (curFormal.getMode() == ParameterMode.ALTERS || curFormal.getMode() == ParameterMode.CLEARS) {
                    newAssumeSubtitutions.put(new PSymbolBuilder(curFormal.asPSymbol())
                            .incoming(true).build(), curActual);
                }
                else {
                    newAssumeSubtitutions.put(curFormal.asPSymbol(), curActual);
                }
            }

            PExp r = newAssume.getTopLevelVariableEqualities().get(functionName.getName());
            if (r != null) {
                returnEnsuresArgSubstitutions.put(e, r.substitute(newAssumeSubtitutions));
            }

            //Assume (T1.Constraint(t) /\ T3.Constraint(v) /\ T6.Constraint(y) /\
            //Post [ t ~> NQV(RP, a), @t ~> a, u ~> Math(exp), v ~> NQV(RP, b),
            //       w ~> c, x ~> d, @y ~> e, @z ~> f]
            block.assume(newAssume.substitute(newAssumeSubtitutions).substitute(returnEnsuresArgSubstitutions));

            //Ok, so this happens down here since the rule is laid out s.t.
            //substitutions occur prior to conjuncting this -- consult the
            //rule and see for yourself
            /* for (ProgParameterSymbol p : op.getParameters()) {
                //T7.Is_Initial(NQV(RP, f));
                //TODO: See todo above
                if (p.getMode() == CLEARS) {
                    PExp initPred =
                            block.g.formInitializationPredicate(
                                    p.getDeclaredType(), p.getNameToken());
                    newAssume = block.g.formConjunct(newAssume, initPred);
                }
            }*/

            //reset the formal param iter in preperation for building the
            //substitution mapping for our confirm
            formalIter = op.getParameters().iterator();
            argIter = e.getArguments().iterator();
            Map<PExp, PExp> confirmSubstitutions = new HashMap<>();
            for (PExp actualArg : e.getArguments()) {
                ProgParameterSymbol curFormal = formalIter.next();
                if (distinguishedModes.contains(curFormal.getMode())) {
                    confirmSubstitutions.put(actualArg, NQV(RP, (PSymbol) actualArg));
                }
            }
            block.finalConfirm(RP.substitute(confirmSubstitutions));
        }
    }

    /** "Next Question-mark Variable" */
    public static PSymbol NQV(PExp RP, PSymbol oldSym) {
        // Add an extra question mark to the front of oldSym
        PSymbol newOldSym = new PSymbolBuilder(oldSym, "?" + oldSym.getName()).build();

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

    @NotNull
    @Override
    public String getDescription() {
        return "general call rule application";
    }

}