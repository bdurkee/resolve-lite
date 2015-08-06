package edu.clemson.resolve.vcgen.model;

import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.vcgen.application.ConfirmApplicationStrategy;

public class VCConfirm extends VCRuleBackedStat {

    public VCConfirm(VCAssertiveBlock.VCAssertiveBlockBuilder block, PExp... e) {
        super(null, block, new ConfirmApplicationStrategy(), e);
    }

    public PExp getConfirmExp() {
        return statComponents.get(0);
    }
}
