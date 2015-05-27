package org.resolvelite.vcgen.model;

import org.antlr.v4.runtime.ParserRuleContext;
import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.vcgen.applicationstrategies.ConfirmApplicationStrategy;
import org.resolvelite.vcgen.applicationstrategies.RuleApplicationStrategy;

public class VCConfirm extends VCRuleBackedStat {

    public VCConfirm(VCAssertiveBlock.VCAssertiveBlockBuilder block,
                     PExp... e) {
        super(null, block, new ConfirmApplicationStrategy(), e);
    }

    public PExp getConfirmExp() {
        return statComponents.get(0);
    }
}
