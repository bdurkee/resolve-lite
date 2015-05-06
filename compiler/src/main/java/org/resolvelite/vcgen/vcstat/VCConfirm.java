package org.resolvelite.vcgen.vcstat;

import org.antlr.v4.runtime.misc.NotNull;
import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.vcgen.applicationstrategies.AssumeApplicationStrategy;
import org.resolvelite.vcgen.applicationstrategies.ConfirmApplicationStrategy;
import org.resolvelite.vcgen.applicationstrategies.RuleApplicationStrategy;

public class VCConfirm extends VCRuleTargetedStat<PExp> {
    public VCConfirm(@NotNull PExp contents,
                    @NotNull RuleApplicationStrategy<PExp> apply,
                    @NotNull AssertiveCode enclosingBlock) {
        super(contents, enclosingBlock, apply);
    }

    public VCConfirm(@NotNull PExp contents,
                    @NotNull AssertiveCode enclosingBlock) {
        this(contents, new ConfirmApplicationStrategy(), enclosingBlock);
    }
}