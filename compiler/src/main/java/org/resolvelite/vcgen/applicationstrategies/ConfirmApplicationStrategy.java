package org.resolvelite.vcgen.applicationstrategies;

import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.vcgen.model.AssertiveCode;
import org.resolvelite.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;

public class ConfirmApplicationStrategy
        implements
            RuleApplicationStrategy<PExp> {

    @Override public AssertiveCode applyRule(PExp statement,
            VCAssertiveBlockBuilder block) {
        block.finalConfirm(block.g.formConjunct(statement,
                block.finalConfirm.getContents()));
        return block.snapshot();
    }

    @Override public String getDescription() {
        return "confirm rule application";
    }
}
