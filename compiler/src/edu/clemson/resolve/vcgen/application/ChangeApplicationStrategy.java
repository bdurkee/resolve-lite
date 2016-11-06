package edu.clemson.resolve.vcgen.application;

import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PSymbol;
import edu.clemson.resolve.vcgen.AssertiveBlock;
import edu.clemson.resolve.vcgen.VCAssertiveBlock;
import edu.clemson.resolve.vcgen.VCAssertiveBlock.VCAssertiveBlockBuilder;

import edu.clemson.resolve.vcgen.VCGenerator;
import edu.clemson.resolve.vcgen.stats.VCChange;
import org.jetbrains.annotations.NotNull;

import java.util.Deque;

public class ChangeApplicationStrategy implements VCStatRuleApplicationStrategy<VCChange> {

    @NotNull
    @Override
    public AssertiveBlock applyRule(@NotNull Deque<VCAssertiveBlockBuilder> branches,
                                    @NotNull VCAssertiveBlockBuilder block,
                                    @NotNull VCChange stat) {
        PExp newFinalConfirm = block.finalConfirm.getConfirmExp();
        for (PSymbol v : stat.getChangeVariables()) {
            PExp primed = VCGenerator.NPV(newFinalConfirm, v);
            newFinalConfirm = newFinalConfirm.substitute(v, primed);
        }
        block.finalConfirm(newFinalConfirm);
        return block.snapshot();
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Change rule application";
    }
}
