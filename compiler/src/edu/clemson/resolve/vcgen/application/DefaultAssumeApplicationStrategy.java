package edu.clemson.resolve.vcgen.application;

import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;
import edu.clemson.resolve.vcgen.model.AssertiveBlock;
import edu.clemson.resolve.vcgen.model.VCAssume;
import edu.clemson.resolve.vcgen.model.VCRuleBackedStat;

import java.util.Arrays;
import java.util.List;

public class DefaultAssumeApplicationStrategy
        implements
            StatRuleApplicationStrategy<VCAssume> {

    @Override public AssertiveBlock applyRule(VCAssertiveBlockBuilder block,
                                              VCAssume stat) {
        PExp curFinalConfirmExp = block.finalConfirm.getConfirmExp();
        PExp assumeExp = stat.getStatComponents().get(0);
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