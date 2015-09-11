package edu.clemson.resolve.vcgen.application;

import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;
import edu.clemson.resolve.vcgen.model.AssertiveBlock;

import java.util.Arrays;
import java.util.List;

public class RememberApplicationStrategy
        implements
            StatRuleApplicationStrategy {

    @Override public AssertiveBlock applyRule(
            VCAssertiveBlockBuilder block, List<PExp> statComponents) {
        PExp confirm = block.finalConfirm.getConfirmExp();
        return block.finalConfirm(confirm.withIncomingSignsErased()).snapshot();
    }

    @Override public AssertiveBlock applyRule(
            VCAssertiveBlockBuilder block, PExp... e) {
        return applyRule(block, Arrays.asList(e));
    }

    @Override public String getDescription() {
        return "remember rule application";
    }
}