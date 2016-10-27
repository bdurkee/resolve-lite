package edu.clemson.resolve.vcgen.model;

import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.vcgen.application.DefaultAssumeApplicationStrategy;
import edu.clemson.resolve.vcgen.application.VCStatRuleApplicationStrategy;
import edu.clemson.resolve.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;
import org.jetbrains.annotations.NotNull;

public class VCAssume extends VCRuleBackedStat {

    protected boolean isStipulatedAssumption = false;
    protected VCStatRuleApplicationStrategy<VCAssume> apply;
    private final PExp assume;

    public VCAssume(VCAssertiveBlockBuilder block, PExp assume) {
        this(block, new DefaultAssumeApplicationStrategy(), assume);
    }

    public VCAssume(VCAssertiveBlockBuilder block,
                    VCStatRuleApplicationStrategy<VCAssume> strategy,
                    PExp assume) {
        super(null, block, strategy);
        this.apply = strategy;
        this.assume = assume;
    }

    public PExp getAssumeExp() {
        return assume;
    }

    @NotNull
    public VCAssume copyWithBlock(@NotNull VCAssertiveBlockBuilder b) {
        return new VCAssume(b, apply, getAssumeExp());
    }
}