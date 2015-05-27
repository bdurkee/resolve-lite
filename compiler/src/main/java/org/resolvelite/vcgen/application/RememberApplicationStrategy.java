package org.resolvelite.vcgen.application;

import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.vcgen.model.AssertiveBlock;
import org.resolvelite.vcgen.model.VCAssertiveBlock;

import java.util.Arrays;
import java.util.List;

public class RememberApplicationStrategy implements StatRuleApplicationStrategy {

    @Override public AssertiveBlock applyRule(
            VCAssertiveBlock.VCAssertiveBlockBuilder block,
            List<PExp> statComponents) {
        PExp confirm = block.finalConfirm.getConfirmExp();
        return block.finalConfirm(confirm.withIncomingSignsErased()).snapshot();
    }

    @Override public AssertiveBlock applyRule(
            VCAssertiveBlock.VCAssertiveBlockBuilder block, PExp... e) {
        return applyRule(block, Arrays.asList(e));
    }

    @Override public String getDescription() {
        return "remember rule application";
    }
}
