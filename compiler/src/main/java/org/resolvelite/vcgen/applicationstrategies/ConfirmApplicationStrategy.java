package org.resolvelite.vcgen.applicationstrategies;

import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.vcgen.model.AssertiveBlock;
import org.resolvelite.vcgen.model.VCAssertiveBlock;
import org.resolvelite.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;

import java.util.Arrays;
import java.util.List;

public class ConfirmApplicationStrategy implements RuleApplicationStrategy {

    @Override public AssertiveBlock applyRule(
            VCAssertiveBlock.VCAssertiveBlockBuilder block, PExp... e) {
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
