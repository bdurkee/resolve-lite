package org.resolvelite.vcgen.model;

import org.antlr.v4.runtime.ParserRuleContext;
import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.vcgen.applicationstrategies.RememberApplicationStrategy;
import org.resolvelite.vcgen.applicationstrategies.RuleApplicationStrategy;

import java.util.Collections;
import java.util.List;

public class VCRemember extends VCRuleBackedStat {

    public VCRemember(VCAssertiveBlock.VCAssertiveBlockBuilder block,
                      PExp... e) {
        super(null, block, new RememberApplicationStrategy(), e);
    }

    public List<PExp> getRememberVars() {
        return statComponents;
    }
}
