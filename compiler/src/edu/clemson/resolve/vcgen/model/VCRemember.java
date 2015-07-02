package edu.clemson.resolve.vcgen.model;

import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.vcgen.application.RememberApplicationStrategy;

import java.util.List;

public class VCRemember extends VCRuleBackedStat {

    public VCRemember(VCAssertiveBlock.VCAssertiveBlockBuilder block, PExp... e) {
        super(null, block, new RememberApplicationStrategy(), e);
    }

    public List<PExp> getRememberVars() {
        return statComponents;
    }
}
