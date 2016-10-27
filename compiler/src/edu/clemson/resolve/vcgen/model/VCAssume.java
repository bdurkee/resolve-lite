package edu.clemson.resolve.vcgen.model;

import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.vcgen.application.DefaultAssumeApplicationStrategy;
import edu.clemson.resolve.vcgen.application.VCStatRuleApplicationStrategy;
import org.jetbrains.annotations.NotNull;

public class VCAssume extends VCRuleBackedStat {

    protected boolean isStipulatedAssumption = false;
    protected VCStatRuleApplicationStrategy<VCAssume> apply;

    public VCAssume(VCAssertiveBlock.VCAssertiveBlockBuilder block, PExp... e) {
        this(block, new DefaultAssumeApplicationStrategy(), e);
    }

    public VCAssume(VCAssertiveBlock.VCAssertiveBlockBuilder block,
                    VCStatRuleApplicationStrategy<VCAssume> strategy,
                    PExp... e) {
        super(null, block, strategy, e);
        this.apply = strategy;
    }

    public PExp getAssumeExp() {
        return statComponents.get(0);
    }

    @NotNull
    public VCAssume copyWithBlock(@NotNull VCAssertiveBlock.VCAssertiveBlockBuilder b) {
        return new VCAssume(b, apply, getAssumeExp());
    }
}