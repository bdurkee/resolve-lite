package edu.clemson.resolve.vcgen.application;

import edu.clemson.resolve.proving.absyn.PApply;
import edu.clemson.resolve.proving.absyn.PExpListener;
import edu.clemson.resolve.vcgen.model.AssertiveBlock;
import edu.clemson.resolve.vcgen.model.VCAssertiveBlock;
import edu.clemson.resolve.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;
import edu.clemson.resolve.vcgen.model.VCRuleBackedStat;
import org.jetbrains.annotations.NotNull;
import org.rsrg.semantics.symbol.OperationSymbol;

import static edu.clemson.resolve.vcgen.application.ExplicitCallApplicationStrategy.*;

public class GeneralCallApplicationStrategy
        implements
            StatRuleApplicationStrategy<VCRuleBackedStat> {

    @Override public AssertiveBlock applyRule(VCAssertiveBlockBuilder block,
                                              VCRuleBackedStat stat) {
        return null;
    }

    public static class GeneralCallRuleSubstitutor extends PExpListener {
        private final VCAssertiveBlockBuilder block;

        public GeneralCallRuleSubstitutor(VCAssertiveBlockBuilder block) {
            this.block = block;
        }

        @Override public void endPApply(@NotNull PApply e) {
            OperationSymbol op = getOperation(block.scope, e);

        }
    }

    @Override public String getDescription() {
        return "general call rule application";
    }
}
