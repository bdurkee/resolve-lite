package org.resolvelite.vcgen.model;

import org.antlr.v4.runtime.ParserRuleContext;
import org.resolvelite.misc.Utils;
import org.resolvelite.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;
import org.resolvelite.vcgen.applicationstrategies.RuleApplicationStrategy;

public class VCCode<T extends ParserRuleContext> extends VCRuleBackedStat<T> {

    public VCCode(T contents, RuleApplicationStrategy<T> apply,
            VCAssertiveBlockBuilder enclosingBlock) {
        super(contents, enclosingBlock, apply);
    }

    public String getText() {
        return Utils.getRawText(getContents());
    }

}
