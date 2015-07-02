package edu.clemson.resolve.vcgen.application;

import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.vcgen.model.AssertiveBlock;
import edu.clemson.resolve.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;

import java.util.Arrays;
import java.util.List;

public class ConfirmApplicationStrategy implements StatRuleApplicationStrategy {

    @Override public AssertiveBlock applyRule(
            VCAssertiveBlockBuilder block, PExp... e) {
        return applyRule(block, Arrays.asList(e));
    }

    @Override public AssertiveBlock applyRule(VCAssertiveBlockBuilder block,
                                              List<PExp> statComponents) {
        block.finalConfirm(block.g.formConjunct(statComponents.get(0),
                block.finalConfirm.getConfirmExp()));
        return block.snapshot();
    }

    @Override public String getDescription() {
        return "confirm rule application";
    }
}
