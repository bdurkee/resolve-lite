package org.resolvelite.vcgen.model;

import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.vcgen.application.RememberApplicationStrategy;

import java.util.List;

public class VCRemember extends VCRuleBackedStat {

    public VCRemember(VCAssertiveBlock.VCAssertiveBlockBuilder block, PExp... e) {
        super(null, block, new RememberApplicationStrategy(), e);
    }

    public List<PExp> getRememberVars() {
        return statComponents;
    }
}
