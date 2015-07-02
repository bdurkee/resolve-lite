package edu.clemson.resolve.vcgen.application;

import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PSymbol;
import edu.clemson.resolve.vcgen.model.AssertiveBlock;
import edu.clemson.resolve.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;

import java.util.Arrays;
import java.util.List;

public class SwapApplicationStrategy implements StatRuleApplicationStrategy {

    @Override public AssertiveBlock applyRule(VCAssertiveBlockBuilder block,
            List<PExp> statComponents) {
        PExp workingConfirm = block.finalConfirm.getConfirmExp();
        PExp swapLeft = statComponents.get(0);
        PExp swapRight = statComponents.get(1);

        PExp temp =
                new PSymbol.PSymbolBuilder("_t;").mathType(swapLeft.getMathType())
                        .build();

        workingConfirm = workingConfirm.substitute(swapRight, temp);
        workingConfirm = workingConfirm.substitute(swapLeft, swapRight);
        workingConfirm = workingConfirm.substitute(temp, swapLeft);
        block.finalConfirm(workingConfirm);
        return block.snapshot();
    }

    @Override public AssertiveBlock applyRule(
            VCAssertiveBlockBuilder block, PExp... e) {
        return applyRule(block, Arrays.asList(e));
    }

    @Override public String getDescription() {
        return "swap rule application";
    }
}