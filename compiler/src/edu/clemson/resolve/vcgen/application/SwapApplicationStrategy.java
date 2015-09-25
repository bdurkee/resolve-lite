package edu.clemson.resolve.vcgen.application;

import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PSymbol;
import edu.clemson.resolve.vcgen.model.AssertiveBlock;
import edu.clemson.resolve.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;
import edu.clemson.resolve.vcgen.model.VCRuleBackedStat;

import java.util.Arrays;
import java.util.List;

public class SwapApplicationStrategy
        implements
            StatRuleApplicationStrategy<VCRuleBackedStat> {

    @Override public AssertiveBlock applyRule(VCAssertiveBlockBuilder block,
            VCRuleBackedStat stat) {
        PExp workingConfirm = block.finalConfirm.getConfirmExp();
        PExp swapLeft = stat.getStatComponents().get(0);
        PExp swapRight = stat.getStatComponents().get(1);

        PExp temp =
                new PSymbol.PSymbolBuilder("_t;").mathType(swapLeft.getMathType())
                        .build();

        workingConfirm = workingConfirm.substitute(swapRight, temp);
        workingConfirm = workingConfirm.substitute(swapLeft, swapRight);
        workingConfirm = workingConfirm.substitute(temp, swapLeft);
        block.finalConfirm(workingConfirm);
        return block.snapshot();
    }

    @Override public String getDescription() {
        return "swap rule application";
    }
}