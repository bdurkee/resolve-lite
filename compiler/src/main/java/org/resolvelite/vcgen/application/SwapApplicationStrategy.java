package org.resolvelite.vcgen.application;

import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.vcgen.model.AssertiveBlock;
import org.resolvelite.vcgen.model.VCAssertiveBlock;
import org.resolvelite.proving.absyn.PSymbol.PSymbolBuilder;

import java.util.Arrays;
import java.util.List;

public class SwapApplicationStrategy implements StatRuleApplicationStrategy {

    @Override public AssertiveBlock applyRule(
            VCAssertiveBlock.VCAssertiveBlockBuilder block,
            List<PExp> statComponents) {
        PExp workingConfirm = block.finalConfirm.getConfirmExp();
        PExp swapLeft = statComponents.get(0);
        PExp swapRight = statComponents.get(1);

        PExp temp =
                new PSymbolBuilder("_t;").mathType(swapLeft.getMathType())
                        .build();

        workingConfirm = workingConfirm.substitute(swapRight, temp);
        workingConfirm = workingConfirm.substitute(swapLeft, swapRight);
        workingConfirm = workingConfirm.substitute(temp, swapLeft);
        block.finalConfirm(workingConfirm);
        return block.snapshot();
    }

    @Override public AssertiveBlock applyRule(
            VCAssertiveBlock.VCAssertiveBlockBuilder block, PExp... e) {
        return applyRule(block, Arrays.asList(e));
    }

    @Override public String getDescription() {
        return null;
    }
}
