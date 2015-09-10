package edu.clemson.resolve.vcgen.model;

import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.vcgen.application.AssumeConfirmApplicationStrategy;

public class VCAssume extends VCRuleBackedStat {

    public VCAssume(VCAssertiveBlock.VCAssertiveBlockBuilder block, PExp... e) {
        super(null, block, new AssumeConfirmApplicationStrategy(), e);
    }

    public PExp getAssumeExp() {
        return statComponents.get(0);
    }
}