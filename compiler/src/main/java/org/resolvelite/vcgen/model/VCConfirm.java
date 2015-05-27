package org.resolvelite.vcgen.model;

import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.vcgen.application.ConfirmApplicationStrategy;

public class VCConfirm extends VCRuleBackedStat {

    public VCConfirm(VCAssertiveBlock.VCAssertiveBlockBuilder block, PExp... e) {
        super(null, block, new ConfirmApplicationStrategy(), e);
    }

    public PExp getConfirmExp() {
        return statComponents.get(0);
    }
}
