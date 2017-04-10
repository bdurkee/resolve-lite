package edu.clemson.resolve.vcgen.app;

import edu.clemson.resolve.misc.Utils;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.vcgen.ListBackedSequent;
import edu.clemson.resolve.vcgen.Sequent;
import edu.clemson.resolve.vcgen.VCAssertiveBlock;
import edu.clemson.resolve.vcgen.VCAssertiveBlock.VCAssertiveBlockBuilder;
import edu.clemson.resolve.vcgen.stats.VCRemember;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class RememberApplicationStrategy implements RuleApplicationStrategy<VCRemember> {

    @NotNull
    @Override
    public VCAssertiveBlock applyRule(@NotNull Deque<VCAssertiveBlockBuilder> accumulator,
                                      @NotNull VCAssertiveBlockBuilder block,
                                      @NotNull VCRemember stat) {
        List<Sequent> sequents = new ArrayList<>();
        for (Sequent s : block.finalConfirm.getSequents()) {
            Sequent t = new ListBackedSequent(
                    Utils.apply(s.getLeftFormulas(), PExp::withIncomingSignsErased),
                    Utils.apply(s.getRightFormulas(), PExp::withIncomingSignsErased));
            sequents.add(t);
        }
        return block.finalConfirm(sequents).snapshot();
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Remember rule app";
    }
}