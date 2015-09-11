package edu.clemson.resolve.vcgen.application;

import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;
import edu.clemson.resolve.vcgen.model.AssertiveBlock;

import java.util.Arrays;
import java.util.List;

public class DefaultAssumeApplicationStrategy
        implements
            StatRuleApplicationStrategy {

    @Override public AssertiveBlock applyRule(
            VCAssertiveBlockBuilder block, PExp... e) {
        return applyRule(block, Arrays.asList(e));
    }

    @Override public AssertiveBlock applyRule(VCAssertiveBlockBuilder block,
                                              List<PExp> statComponents) {
        PExp curFinalConfirmExp = block.finalConfirm.getConfirmExp();
        PExp assumeExp = statComponents.get(0);
        if ( curFinalConfirmExp.isObviouslyTrue() ) {
            block.finalConfirm(assumeExp);
        }
        else if ( !assumeExp.equals(block.g.getTrueExp()) ) {
            block.finalConfirm(block.g.formImplies(assumeExp, curFinalConfirmExp));
        }
        return block.snapshot();
    }

    @Override public String getDescription() {
        return "assume confirm rule application";
    }
}
