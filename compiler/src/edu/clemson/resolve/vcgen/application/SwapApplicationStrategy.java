package edu.clemson.resolve.vcgen.application;

import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PSymbol;
import edu.clemson.resolve.vcgen.AssertiveBlock;
import edu.clemson.resolve.vcgen.VCAssertiveBlock.VCAssertiveBlockBuilder;
import edu.clemson.resolve.vcgen.stats.VCSwap;
import org.jetbrains.annotations.NotNull;

import java.util.Deque;

public class SwapApplicationStrategy implements VCStatRuleApplicationStrategy<VCSwap> {

    @NotNull
    @Override
    public AssertiveBlock applyRule(@NotNull Deque<VCAssertiveBlockBuilder> accumulator,
                                    @NotNull VCAssertiveBlockBuilder block,
                                    @NotNull VCSwap stat) {
        PExp workingConfirm = block.finalConfirm.getConfirmExp();
        PExp swapLeft = stat.getLeft();
        PExp swapRight = stat.getRight();
        PExp temp = new PSymbol.PSymbolBuilder((PSymbol)swapLeft).name("_t;").build();

        workingConfirm = workingConfirm.substitute(swapRight, temp);
        workingConfirm = workingConfirm.substitute(swapLeft, swapRight);
        workingConfirm = workingConfirm.substitute(temp, swapLeft);
        block.finalConfirm(workingConfirm);
        return block.snapshot();
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Swap rule application";
    }
}