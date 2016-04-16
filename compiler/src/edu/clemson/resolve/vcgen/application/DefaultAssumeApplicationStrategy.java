package edu.clemson.resolve.vcgen.application;

import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.vcgen.model.AssertiveBlock;
import edu.clemson.resolve.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;
import edu.clemson.resolve.vcgen.model.VCAssume;
import org.jetbrains.annotations.NotNull;

public class DefaultAssumeApplicationStrategy
        implements
            StatRuleApplicationStrategy<VCAssume> {

    @NotNull @Override public AssertiveBlock applyRule(
            @NotNull VCAssertiveBlockBuilder block,
            @NotNull VCAssume stat) {
        PExp curFinalConfirmExp = block.finalConfirm.getConfirmExp();
        PExp assumeExp = stat.getStatComponents().get(0);
        if (curFinalConfirmExp.isObviouslyTrue()) {
            block.finalConfirm(assumeExp);
        }
        else if (!assumeExp.equals(block.g.getTrueExp())) {
            block.finalConfirm(block.g.formImplies(assumeExp, curFinalConfirmExp));
        }
        return block.snapshot();
    }

    @NotNull @Override public String getDescription() {
        return "assume confirm rule application";
    }
}
