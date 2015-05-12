package org.resolvelite.vcgen.applicationstrategies;

import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.vcgen.model.AssertiveCode;
import org.resolvelite.vcgen.model.VCAssertiveBlock;

public class RememberApplicationStrategy
        implements
            RuleApplicationStrategy<Void> {

    @Override public AssertiveCode applyRule(Void statement,
            VCAssertiveBlock.VCAssertiveBlockBuilder block) {
        PExp confirm = block.finalConfirm.getContents();
        return block.finalConfirm(confirm.withIncomingVariablesRemoved())
                .snapshot();
    }

    @Override public String getDescription() {
        return "remember rule application";
    }
}
