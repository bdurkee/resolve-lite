package edu.clemson.resolve.vcgen.application;

import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;
import edu.clemson.resolve.vcgen.model.AssertiveBlock;

import java.util.Arrays;
import java.util.List;

public class AssumeConfirmApplicationStrategy
        implements
            StatRuleApplicationStrategy {

    @Override public AssertiveBlock applyRule(
            VCAssertiveBlockBuilder block, PExp... e) {
        return applyRule(block, Arrays.asList(e));
    }

    @Override public AssertiveBlock applyRule(VCAssertiveBlockBuilder block,
                                              List<PExp> statComponents) {
        PExp curFinalConfirm = block.finalConfirm.getConfirmExp();
        PExp statement = statComponents.get(0);

        if ( curFinalConfirm.isObviouslyTrue() ) {
            block.finalConfirm(statement);
        }
        else if ( !statement.equals(block.g.getTrueExp()) ) {
            block.finalConfirm(block.g.formImplies(statement, curFinalConfirm));
        }
        return block.snapshot();
    }

    @Override public String getDescription() {
        return "assume confirm rule application";
    }
}
