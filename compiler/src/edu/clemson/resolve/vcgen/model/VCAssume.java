package edu.clemson.resolve.vcgen.model;

import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.vcgen.application.AssumeApplicationStrategy;

public class VCAssume extends VCRuleBackedStat {

    public VCAssume(VCAssertiveBlock.VCAssertiveBlockBuilder block, PExp... e) {
        super(null, block, new AssumeApplicationStrategy(), e);
    }

    public PExp getAssumeExp() {
        return statComponents.get(0);
    }
}