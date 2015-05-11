package org.resolvelite.vcgen.model;

import org.antlr.v4.runtime.ParserRuleContext;
import org.resolvelite.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;
import org.resolvelite.vcgen.applicationstrategies.RuleApplicationStrategy;

public class VCStat<T extends ParserRuleContext> extends VCRuleBackedStat<T> {

    public VCStat(T contents, RuleApplicationStrategy<T> apply,
                  VCAssertiveBlockBuilder enclosingBlock) {
        super(contents, enclosingBlock, apply);
    }

}
