package org.resolvelite.vcgen.applicationstrategies;

import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.vcgen.model.AssertiveCode;
import org.resolvelite.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;

public class AssumeApplicationStrategy implements RuleApplicationStrategy<PExp> {

    //stub impl for now
    @Override public AssertiveCode applyRule(PExp statement,
            VCAssertiveBlockBuilder block) {
        return block.snapshot();
    }

    @Override public String getDescription() {
        return "assume rule application";
    }
}
