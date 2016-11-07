package edu.clemson.resolve.vcgen.application;

import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.semantics.DumbMathClssftnHandler;
import edu.clemson.resolve.vcgen.VCAssertiveBlock;
import edu.clemson.resolve.vcgen.VCAssertiveBlock.VCAssertiveBlockBuilder;
import edu.clemson.resolve.vcgen.stats.VCConfirm;
import org.jetbrains.annotations.NotNull;

import java.util.Deque;

public class ConfirmApplicationStrategy implements VCStatRuleApplicationStrategy<VCConfirm> {

    @NotNull
    @Override
    public VCAssertiveBlock applyRule(@NotNull Deque<VCAssertiveBlockBuilder> accumulator,
                                      @NotNull VCAssertiveBlockBuilder block,
                                      @NotNull VCConfirm stat) {
        PExp newFinalConfirm = null;
        DumbMathClssftnHandler g = block.g;

        if (block.finalConfirm.getConfirmExp().equals(g.getTrueExp())) {
            newFinalConfirm = stat.getConfirmExp();
        }
        else {
            if (stat.getConfirmExp().equals(g.getTrueExp())) {
                newFinalConfirm = block.finalConfirm.getConfirmExp(); //no need to conjuct true to the final conf
            }
            else {
                newFinalConfirm = g.formConjunct(stat.getConfirmExp(), block.finalConfirm.getConfirmExp());
            }
        }
        block.finalConfirm(newFinalConfirm);
        return block.snapshot();
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Confirm rule application";
    }
}
