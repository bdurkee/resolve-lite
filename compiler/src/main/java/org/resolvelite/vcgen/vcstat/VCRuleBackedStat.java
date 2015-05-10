package org.resolvelite.vcgen.vcstat;

import org.antlr.v4.runtime.misc.NotNull;
import org.resolvelite.codegen.model.OutputModelObject;
import org.resolvelite.vcgen.vcstat.VCAssertiveBlock.VCAssertiveBlockBuilder;
import org.resolvelite.vcgen.applicationstrategies.RuleApplicationStrategy;

public abstract class VCRuleBackedStat<T> extends OutputModelObject {

    private final T contents;
    private final RuleApplicationStrategy<T> applicationStrategy;
    private final VCAssertiveBlockBuilder enclosingBlock;

    public VCRuleBackedStat(T contents, VCAssertiveBlockBuilder block,
            RuleApplicationStrategy<T> apply) {
        this.contents = contents;
        this.applicationStrategy = apply;
        this.enclosingBlock = block;
    }

    public AssertiveCode reduce() {
        return applicationStrategy.applyRule(contents, enclosingBlock);
    }

    public T getContents() {
        return contents;
    }

    public VCAssertiveBlockBuilder getEnclosingBlock() {
        return enclosingBlock;
    }
}
