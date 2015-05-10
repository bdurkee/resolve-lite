package org.resolvelite.vcgen.vcstat;

import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.vcgen.applicationstrategies.ConfirmApplicationStrategy;
import org.resolvelite.vcgen.vcstat.VCAssertiveBlock.VCAssertiveBlockBuilder;
import org.resolvelite.vcgen.applicationstrategies.RuleApplicationStrategy;

public class VCConfirm extends VCRuleBackedStat<PExp> {
    public VCConfirm(PExp contents, RuleApplicationStrategy<PExp> apply,
            VCAssertiveBlockBuilder enclosingBlock) {
        super(contents, enclosingBlock, apply);
    }

    public VCConfirm(PExp contents, VCAssertiveBlockBuilder enclosingBlock) {
        this(contents, new ConfirmApplicationStrategy(), enclosingBlock);
    }

    @Override public String toString() {
        return "confirm: " + getContents() + ";";
    }
}