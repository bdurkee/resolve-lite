package org.resolvelite.vcgen.vcstat;

import org.antlr.v4.runtime.ParserRuleContext;
import org.resolvelite.vcgen.vcstat.VCAssertiveBlock.VCAssertiveBlockBuilder;
import org.resolvelite.vcgen.applicationstrategies.RuleApplicationStrategy;

public class VCCode<T extends ParserRuleContext> extends VCRuleBackedStat<T> {

    public VCCode(T contents, RuleApplicationStrategy<T> apply,
            VCAssertiveBlockBuilder enclosingBlock) {
        super(contents, enclosingBlock, apply);
    }

    @Override public String toString() {
        return getContents().getText();
    }
}
