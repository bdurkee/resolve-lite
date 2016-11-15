package edu.clemson.resolve.vcgen.app;

import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PSymbol;
import edu.clemson.resolve.vcgen.VCAssertiveBlock;
import edu.clemson.resolve.vcgen.VCAssertiveBlock.VCAssertiveBlockBuilder;
import edu.clemson.resolve.vcgen.stats.VCConfirm;
import edu.clemson.resolve.vcgen.stats.VCSwap;
import org.jetbrains.annotations.NotNull;

import java.util.Deque;

public class SwapApplicationStrategy implements RuleApplicationStrategy<VCSwap> {

    @NotNull
    @Override
    public VCAssertiveBlock applyRule(@NotNull Deque<VCAssertiveBlockBuilder> accumulator,
                                      @NotNull VCAssertiveBlockBuilder block,
                                      @NotNull VCSwap stat) {
        PExp swapLeft = stat.getLeft();
        PExp swapRight = stat.getRight();
        PExp temp = new PSymbol.PSymbolBuilder((PSymbol)swapLeft).name("_t;").build();
        VCConfirm workingConfirm = block.finalConfirm;

        workingConfirm = workingConfirm.withSequentFormulaSubstitution(swapRight, temp);
        workingConfirm = workingConfirm.withSequentFormulaSubstitution(swapLeft, swapRight);
        workingConfirm = workingConfirm.withSequentFormulaSubstitution(temp, swapLeft);
        block.finalConfirm(workingConfirm.getSequents());
        return block.snapshot();
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Swap rule app";
    }
}