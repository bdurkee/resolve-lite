package edu.clemson.resolve.vcgen.application;

import edu.clemson.resolve.vcgen.model.AssertiveBlock;
import edu.clemson.resolve.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;
import edu.clemson.resolve.vcgen.model.VCRuleBackedStat;
import org.jetbrains.annotations.NotNull;

public class SwapApplicationStrategy
        implements
        StatRuleApplicationStrategy<VCRuleBackedStat> {

    //TODO: Todo, maybe make vcswapStat, vcwh
    @NotNull
    @Override
    public AssertiveBlock applyRule(
            @NotNull VCAssertiveBlockBuilder block,
            @NotNull VCRuleBackedStat stat) {
       /* PExp workingConfirm = block.finalConfirm.getConfirmExp();
        PExp swapLeft = stat.getStatComponents().get(0);
        PExp swapRight = stat.getStatComponents().get(1);

        PExp temp = new PSymbolBuilder((PSymbol)swapLeft).name("_t;").build();

        workingConfirm = workingConfirm.substitute(swapRight, temp);
        workingConfirm = workingConfirm.substitute(swapLeft, swapRight);
        workingConfirm = workingConfirm.substitute(temp, swapLeft);
        block.finalConfirm(workingConfirm);*/
        return block.snapshot();
    }

    @NotNull
    @Override
    public String getDescription() {
        return "swap rule application";
    }
}