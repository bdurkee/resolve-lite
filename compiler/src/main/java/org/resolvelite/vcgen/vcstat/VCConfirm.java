package org.resolvelite.vcgen.vcstat;

import org.antlr.v4.runtime.misc.NotNull;
import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.vcgen.applicationstrategies.AssumeApplicationStrategy;
import org.resolvelite.vcgen.applicationstrategies.ConfirmApplicationStrategy;
import org.resolvelite.vcgen.applicationstrategies.RuleApplicationStrategy;

public class VCConfirm extends VCRuleTargetedStat<PExp> {
    public VCConfirm(PExp contents, RuleApplicationStrategy<PExp> apply,
            AssertiveCode enclosingBlock) {
        super(contents, enclosingBlock, apply);
    }

    public VCConfirm(PExp contents, AssertiveCode enclosingBlock) {
        this(contents, new ConfirmApplicationStrategy(), enclosingBlock);
    }
}