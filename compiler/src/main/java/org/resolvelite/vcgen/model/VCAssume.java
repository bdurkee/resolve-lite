package org.resolvelite.vcgen.model;

import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.vcgen.applicationstrategies.AssumeApplicationStrategy;
import org.resolvelite.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;
import org.resolvelite.vcgen.applicationstrategies.RuleApplicationStrategy;

public class VCAssume extends VCRuleBackedStat<PExp> {
    public VCAssume(PExp contents, RuleApplicationStrategy<PExp> apply,
            VCAssertiveBlock.VCAssertiveBlockBuilder enclosingBlock) {
        super(contents, enclosingBlock, apply);
    }

    public VCAssume(PExp contents, VCAssertiveBlockBuilder enclosingBlock) {
        this(contents, new AssumeApplicationStrategy(), enclosingBlock);
    }

    @Override public String getText() {
        return getContents().toString();
    }

}