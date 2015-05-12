package org.resolvelite.vcgen.model;

import org.resolvelite.misc.Utils;
import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.vcgen.applicationstrategies.RememberApplicationStrategy;
import org.resolvelite.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;
import org.resolvelite.vcgen.applicationstrategies.RuleApplicationStrategy;

public class VCRemember extends VCRuleBackedStat<Void> {

    public VCRemember(Void contents, VCAssertiveBlockBuilder block) {
        super(contents, block, new RememberApplicationStrategy());
    }

    public String getText() {
        return "";
    }

}
