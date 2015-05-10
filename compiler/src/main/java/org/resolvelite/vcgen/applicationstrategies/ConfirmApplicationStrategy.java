package org.resolvelite.vcgen.applicationstrategies;

import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.vcgen.vcstat.AssertiveCode;
import org.resolvelite.vcgen.vcstat.VCAssertiveBlock;
import org.resolvelite.vcgen.vcstat.VCAssertiveBlock.VCAssertiveBlockBuilder;
import org.resolvelite.vcgen.vcstat.VCConfirm;

public class ConfirmApplicationStrategy
        implements
            RuleApplicationStrategy<PExp> {

    @Override public AssertiveCode applyRule(PExp statement,
            VCAssertiveBlockBuilder block) {
        return block.snapshot();
    }

}
