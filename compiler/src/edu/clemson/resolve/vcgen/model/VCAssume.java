package edu.clemson.resolve.vcgen.model;

import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.vcgen.application.DefaultAssumeApplicationStrategy;
import edu.clemson.resolve.vcgen.application.StatRuleApplicationStrategy;

public class VCAssume extends VCRuleBackedStat {

    protected boolean isStipulatedAssumption = false;

    public VCAssume(VCAssertiveBlock.VCAssertiveBlockBuilder block, PExp... e) {
        this(block, new DefaultAssumeApplicationStrategy(), e);
    }

    public VCAssume(VCAssertiveBlock.VCAssertiveBlockBuilder block,
                    StatRuleApplicationStrategy<VCAssume> strategy,
                    PExp... e) {
        super(null, block, strategy, e);
    }

    public PExp getAssumeExp() {
        return statComponents.get(0);
    }
}