package edu.clemson.resolve.vcgen.application;

import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;
import edu.clemson.resolve.vcgen.model.AssertiveBlock;
import edu.clemson.resolve.vcgen.model.VCRemember;
import edu.clemson.resolve.vcgen.model.VCRuleBackedStat;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class RememberApplicationStrategy
        implements
            StatRuleApplicationStrategy<VCRemember> {

    @NotNull
    @Override public AssertiveBlock applyRule(
            @NotNull VCAssertiveBlockBuilder block, @NotNull VCRemember stat) {
        PExp confirm = block.finalConfirm.getConfirmExp();
        return block.finalConfirm(confirm.withIncomingSignsErased()).snapshot();
    }

    @NotNull
    @Override public String getDescription() {
        return "remember rule application";
    }
}