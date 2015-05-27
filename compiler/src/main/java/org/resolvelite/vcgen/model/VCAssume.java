package org.resolvelite.vcgen.model;

import org.antlr.v4.runtime.ParserRuleContext;
import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.vcgen.applicationstrategies.AssumeApplicationStrategy;
import org.resolvelite.vcgen.applicationstrategies.RuleApplicationStrategy;

public class VCAssume extends VCRuleBackedStat {

    public VCAssume(VCAssertiveBlock.VCAssertiveBlockBuilder block, PExp... e) {
        super(null, block, new AssumeApplicationStrategy(), e);
    }

    public PExp getAssumeExp() {
        return statComponents.get(0);
    }
}
