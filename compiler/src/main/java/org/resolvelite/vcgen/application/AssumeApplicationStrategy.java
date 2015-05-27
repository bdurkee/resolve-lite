package org.resolvelite.vcgen.application;

import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.vcgen.model.AssertiveBlock;
import org.resolvelite.vcgen.model.VCAssertiveBlock;
import org.resolvelite.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;

import java.util.Arrays;
import java.util.List;

public class AssumeApplicationStrategy implements RuleApplicationStrategy {

    @Override public AssertiveBlock applyRule(
            VCAssertiveBlock.VCAssertiveBlockBuilder block, PExp... e) {
        return applyRule(block, Arrays.asList(e));
    }

    @Override public AssertiveBlock applyRule(VCAssertiveBlockBuilder block,
                                              List<PExp> statComponents) {
        PExp curFinalConfirm = block.finalConfirm.getConfirmExp();
        PExp statement = statComponents.get(0);

        if ( curFinalConfirm.isLiteralTrue() ) {
            block.finalConfirm(statement);
        }
        else if ( !statement.equals(block.g.getTrueExp()) ) {
            block.finalConfirm(block.g.formImplies(statement, curFinalConfirm));
        }
        return block.snapshot();
    }

    @Override public String getDescription() {
        return "assume rule application";
    }
}
