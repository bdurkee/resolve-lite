package edu.clemson.resolve.vcgen.application;

import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.vcgen.model.AssertiveBlock;
import edu.clemson.resolve.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;
import edu.clemson.resolve.vcgen.model.VCConfirm;
import org.jetbrains.annotations.NotNull;

public class ConfirmApplicationStrategy
        implements
            StatRuleApplicationStrategy<VCConfirm> {

    @NotNull @Override public AssertiveBlock applyRule(
            @NotNull VCAssertiveBlockBuilder block,
            @NotNull VCConfirm stat) {
        PExp e = block.g.formConjunct(stat.getStatComponents().get(0),
                block.finalConfirm.getConfirmExp());
        block.finalConfirm(e);
        return block.snapshot();
    }

    @NotNull @Override public String getDescription() {
        return "confirm rule application";
    }
}
