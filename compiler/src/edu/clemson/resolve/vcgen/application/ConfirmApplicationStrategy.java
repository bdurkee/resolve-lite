package edu.clemson.resolve.vcgen.application;

import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.vcgen.model.AssertiveBlock;
import edu.clemson.resolve.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;
import edu.clemson.resolve.vcgen.model.VCRuleBackedStat;

import java.util.Arrays;
import java.util.List;

public class ConfirmApplicationStrategy implements StatRuleApplicationStrategy {

    @Override public AssertiveBlock applyRule(VCAssertiveBlockBuilder block,
                                              VCRuleBackedStat stat) {
        PExp e = block.g.formConjunct(stat.getStatComponents().get(0),
                block.finalConfirm.getConfirmExp());
        block.finalConfirm(e);
        return block.snapshot();
    }

    @Override public String getDescription() {
        return "confirm rule application";
    }
}
