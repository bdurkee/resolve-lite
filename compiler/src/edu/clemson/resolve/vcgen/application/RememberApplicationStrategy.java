package edu.clemson.resolve.vcgen.application;

import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.vcgen.AssertiveBlock;
import edu.clemson.resolve.vcgen.VCAssertiveBlock.VCAssertiveBlockBuilder;
import edu.clemson.resolve.vcgen.stats.VCRemember;
import org.jetbrains.annotations.NotNull;

public class RememberApplicationStrategy implements VCStatRuleApplicationStrategy<VCRemember> {

    @NotNull
    @Override
    public AssertiveBlock applyRule(@NotNull VCAssertiveBlockBuilder block, @NotNull VCRemember stat) {
        PExp confirm = block.finalConfirm.getConfirmExp();
        return block.finalConfirm(confirm.withIncomingSignsErased()).snapshot();
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Remember rule application";
    }
}