package edu.clemson.resolve.vcgen.app;

import edu.clemson.resolve.vcgen.Sequent;
import edu.clemson.resolve.vcgen.VCAssertiveBlock;
import edu.clemson.resolve.vcgen.VCAssertiveBlock.VCAssertiveBlockBuilder;
import edu.clemson.resolve.vcgen.stats.VCConfirm;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class ConfirmApplicationStrategy implements RuleApplicationStrategy<VCConfirm> {

    @NotNull
    @Override
    public VCAssertiveBlock applyRule(@NotNull Deque<VCAssertiveBlockBuilder> accumulator,
                                      @NotNull VCAssertiveBlockBuilder block,
                                      @NotNull VCConfirm stat) {
        List<Sequent> combined = new ArrayList<>(stat.getSequents());
        combined.addAll(block.finalConfirm.getSequents());
        return block.finalConfirm(combined).snapshot();
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Confirm rule app";
    }
}
