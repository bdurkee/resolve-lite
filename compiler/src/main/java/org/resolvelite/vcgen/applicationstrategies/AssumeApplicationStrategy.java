package org.resolvelite.vcgen.applicationstrategies;

import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.vcgen.model.AssertiveCode;
import org.resolvelite.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;
import org.resolvelite.vcgen.model.VCConfirm;

public class AssumeApplicationStrategy implements RuleApplicationStrategy<PExp> {

    @Override public AssertiveCode applyRule(PExp statement,
            VCAssertiveBlockBuilder block) {
        PExp curFinalConfirm = block.finalConfirm.getContents();
        if ( statement != null && !statement.equals(block.g.getTrueExp()) ) {
            block.finalConfirm(block.g.formImplies(statement, curFinalConfirm));
        }
        return block.snapshot();
    }

    @Override public String getDescription() {
        return "assume rule application";
    }
}
