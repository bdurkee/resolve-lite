package edu.clemson.resolve.vcgen.application;

import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.vcgen.model.AssertiveBlock;
import edu.clemson.resolve.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;
import edu.clemson.resolve.vcgen.model.VCConfirm;
import edu.clemson.resolve.vcgen.model.VCRuleBackedStat;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ConfirmApplicationStrategy
        implements
            StatRuleApplicationStrategy<VCConfirm> {

    @Override public AssertiveBlock applyRule(VCAssertiveBlockBuilder block,
                                              VCConfirm stat) {
        PExp e = block.g.formConjunct(stat.getStatComponents().get(0),
                block.finalConfirm.getConfirmExp());
        block.finalConfirm(e);
        return block.snapshot();
    }

    @Override public String getDescription() {
        return "confirm rule application";
    }
}
