package edu.clemson.resolve.vcgen.app;

import edu.clemson.resolve.vcgen.VCAssertiveBlock;
import edu.clemson.resolve.vcgen.VCAssertiveBlock.VCAssertiveBlockBuilder;
import edu.clemson.resolve.vcgen.stats.VCSwap;
import org.jetbrains.annotations.NotNull;

import java.util.Deque;

public class SwapApplicationStrategy implements VCStatRuleApplicationStrategy<VCSwap> {

    @NotNull
    @Override
    public VCAssertiveBlock applyRule(@NotNull Deque<VCAssertiveBlockBuilder> accumulator,
                                      @NotNull VCAssertiveBlockBuilder block,
                                      @NotNull VCSwap stat) {
        /*PExp workingConfirm = block.finalConfirm.getConfirmExp();
        PExp swapLeft = stat.getLeft();
        PExp swapRight = stat.getRight();
        PExp temp = new PSymbol.PSymbolBuilder((PSymbol)swapLeft).name("_t;").build();

        workingConfirm = workingConfirm.substitute(swapRight, temp);
        workingConfirm = workingConfirm.substitute(swapLeft, swapRight);
        workingConfirm = workingConfirm.substitute(temp, swapLeft);
        block.finalConfirm(workingConfirm);*/
        return block.snapshot();
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Swap rule app";
    }
}