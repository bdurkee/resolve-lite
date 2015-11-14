package edu.clemson.resolve.vcgen.application;

import edu.clemson.resolve.proving.absyn.PApply;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PExpListener;
import edu.clemson.resolve.vcgen.model.AssertiveBlock;
import edu.clemson.resolve.vcgen.model.VCAssertiveBlock;
import edu.clemson.resolve.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;
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
                if (CONSTRAINT_REQUIRING_MODES.contains(p.getMode())) {
                    if (p.getDeclaredType() instanceof PTFamily) {
                        curAssume = block.g.formConjunct(curAssume,
                                ((PTFamily) p.getDeclaredType())
                                        .getInitializationEnsures());
                    }
                }
            }
            Map<PExp, PExp> assumeSubstitutions = new HashMap<>();
            Iterator<ProgParameterSymbol> formalParamIter =
                    op.getParameters().iterator();
            Iterator<PExp> argIter = e.getArguments().iterator();

            while (formalParamIter.hasNext()) {
                ProgParameterSymbol curParam = formalParamIter.next();
                PExp curActual = argIter.next();
                if (curParam.getMode() == UPDATES) {

                }
            }
        }
    }

    @Override public String getDescription() {
        return "general call rule application";
    }
}
