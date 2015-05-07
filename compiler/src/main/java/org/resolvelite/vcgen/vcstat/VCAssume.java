package org.resolvelite.vcgen.vcstat;

import org.antlr.v4.runtime.misc.NotNull;
import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.vcgen.applicationstrategies.AssumeApplicationStrategy;
import org.resolvelite.vcgen.applicationstrategies.RuleApplicationStrategy;

public class VCAssume extends VCRuleTargetedStat<PExp> {
    public VCAssume(PExp contents, RuleApplicationStrategy<PExp> apply,
            AssertiveCode enclosingBlock) {
        super(contents, enclosingBlock, apply);
    }

    public VCAssume(PExp contents, AssertiveCode enclosingBlock) {
        this(contents, new AssumeApplicationStrategy(), enclosingBlock);
    }
}