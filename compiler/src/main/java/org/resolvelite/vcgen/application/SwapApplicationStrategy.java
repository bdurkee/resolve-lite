package org.resolvelite.vcgen.application;

import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.vcgen.model.AssertiveBlock;
import org.resolvelite.vcgen.model.VCAssertiveBlock;

import java.util.List;

public class SwapApplicationStrategy implements RuleApplicationStrategy {

    @Override public AssertiveBlock applyRule(
            VCAssertiveBlock.VCAssertiveBlockBuilder block,
            List<PExp> statComponents) {
        return null;
    }

    @Override public AssertiveBlock applyRule(
            VCAssertiveBlock.VCAssertiveBlockBuilder block, PExp... e) {
        return null;
    }

    @Override public String getDescription() {
        return null;
    }
}
