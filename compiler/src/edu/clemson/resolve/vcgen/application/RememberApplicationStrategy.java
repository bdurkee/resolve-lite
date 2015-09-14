package edu.clemson.resolve.vcgen.application;

import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;
import edu.clemson.resolve.vcgen.model.AssertiveBlock;
import edu.clemson.resolve.vcgen.model.VCRemember;
import edu.clemson.resolve.vcgen.model.VCRuleBackedStat;

import java.util.Arrays;
import java.util.List;

public class RememberApplicationStrategy
        implements
            StatRuleApplicationStrategy<VCRemember> {

    @Override public AssertiveBlock applyRule(
            VCAssertiveBlockBuilder block, VCRemember stat) {
        PExp confirm = block.finalConfirm.getConfirmExp();
        return block.finalConfirm(confirm.withIncomingSignsErased()).snapshot();
    }

    @Override public String getDescription() {
        return "remember rule application";
    }
}