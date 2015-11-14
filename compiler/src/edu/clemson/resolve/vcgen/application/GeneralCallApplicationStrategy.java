package edu.clemson.resolve.vcgen.application;

import edu.clemson.resolve.proving.absyn.PApply;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PExpListener;
import edu.clemson.resolve.proving.absyn.PSymbol;
import edu.clemson.resolve.proving.absyn.PSymbol.PSymbolBuilder;
import edu.clemson.resolve.vcgen.model.AssertiveBlock;
import edu.clemson.resolve.vcgen.model.VCAssertiveBlock;
import edu.clemson.resolve.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;
import edu.clemson.resolve.vcgen.model.VCConfirm;
import edu.clemson.resolve.vcgen.model.VCRuleBackedStat;
import org.jetbrains.annotations.NotNull;
import org.rsrg.semantics.programtype.PTFamily;
import org.rsrg.semantics.programtype.PTNamed;
import org.rsrg.semantics.symbol.OperationSymbol;
import org.rsrg.semantics.symbol.ProgParameterSymbol;
import org.rsrg.semantics.symbol.ProgParameterSymbol.ParameterMode;

import java.util.*;

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
            final Set<ParameterMode> CONSTRAINT_REQUIRING_MODES =
                    new HashSet<>(Arrays.asList(UPDATES, REPLACES, ALTERS));
            PExp curAssume = op.getEnsures();
            for (ProgParameterSymbol p : op.getParameters()) {
                //T1.Constraint(t) /\ T3.Constraint(v) /\ T6.Constraint(y) /\
                //postcondition
                if (CONSTRAINT_REQUIRING_MODES.contains(p.getMode())) {
                    if (p.getDeclaredType() instanceof PTFamily) {
                        curAssume = block.g.formConjunct(curAssume,
                                ((PTFamily) p.getDeclaredType())
                                        .getInitializationEnsures());
                    }
                }
                //T7.Is_Initial(NQV(RP, f));
                if (p.getMode() == CLEARS) {
                    
                }
            }
            PExp RP = block.finalConfirm.getConfirmExp();
            Map<PExp, PExp> assumeSubstitutions = new HashMap<>();
            Iterator<ProgParameterSymbol> formalParamIter =
                    op.getParameters().iterator();
            Iterator<PExp> argIter = e.getArguments().iterator();

            while (formalParamIter.hasNext()) {
                ProgParameterSymbol curFormal = formalParamIter.next();
                PExp curActual = (PSymbol)argIter.next();

                //t ~> NQV(RP, a), @t ~> a
                if (curFormal.getMode() == UPDATES) {
                    assumeSubstitutions.put(curFormal.asPSymbol(),
                            createQuestionMarkVariable(RP, (PSymbol)curActual));
                    assumeSubstitutions.put(new PSymbolBuilder(curFormal
                                    .asPSymbol()).incoming(true).build(),
                            (PSymbol)curActual);
                }
                //v ~> NQV(RP, b)
                else if (curFormal.getMode() == REPLACES) {
                    assumeSubstitutions.put(curFormal.asPSymbol(),
                            createQuestionMarkVariable(RP, (PSymbol)curActual));
                }
                //@y ~> e, @z ~> f
                else if (curFormal.getMode() == ALTERS ||
                        curFormal.getMode() == CLEARS) {
                    assumeSubstitutions.put(curFormal.asPSymbol(),
                            createQuestionMarkVariable(RP, (PSymbol)curActual));
                }

            }
        }
    }



    public static PSymbol createQuestionMarkVariable(PExp RP, PSymbol oldSym) {
        // Add an extra question mark to the front of oldSym
        PSymbol newOldSym = new PSymbolBuilder(oldSym, "?"+oldSym.getName())
                .build();

        // Applies the question mark to oldVar if it is our first time visiting.
        if (RP.containsName(oldSym.getName())) {
            return createQuestionMarkVariable(RP, newOldSym);
        }
        // Don't need to apply the question mark here.
        else if (RP.containsName(newOldSym.getName())) {
            return createQuestionMarkVariable(RP, newOldSym);
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
