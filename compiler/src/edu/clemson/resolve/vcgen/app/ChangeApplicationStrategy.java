package edu.clemson.resolve.vcgen.app;

import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PSymbol;
import edu.clemson.resolve.vcgen.Sequent;
import edu.clemson.resolve.vcgen.VCAssertiveBlock;
import edu.clemson.resolve.vcgen.VCAssertiveBlock.VCAssertiveBlockBuilder;

import edu.clemson.resolve.vcgen.VCGen;
import edu.clemson.resolve.vcgen.stats.VCChange;
import edu.clemson.resolve.vcgen.stats.VCConfirm;
import org.jetbrains.annotations.NotNull;

import java.util.Deque;
import java.util.List;
import java.util.Set;

public class ChangeApplicationStrategy implements VCStatRuleApplicationStrategy<VCChange> {

    @NotNull
    @Override
    public VCAssertiveBlock applyRule(@NotNull Deque<VCAssertiveBlockBuilder> branches,
                                      @NotNull VCAssertiveBlockBuilder block,
                                      @NotNull VCChange stat) {
        VCConfirm workingConfirm = block.finalConfirm;
        Set<Sequent> existingSequents = workingConfirm.getSequents();
        for (PSymbol v : stat.getChangeVariables()) {
            PExp primed = VCGen.NPV(existingSequents, v);
            workingConfirm = workingConfirm.withSequentFormulaSubstitution(v, primed);
        }
        return block.finalConfirm(workingConfirm).snapshot();
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Change rule application";
    }
}