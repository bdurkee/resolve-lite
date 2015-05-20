package org.resolvelite.vcgen.model;

import org.resolvelite.codegen.model.OutputModelObject;
import org.resolvelite.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;
import org.resolvelite.vcgen.applicationstrategies.RuleApplicationStrategy;

public abstract class VCRuleBackedStat<T> extends OutputModelObject {

    protected final T contents;
    protected final RuleApplicationStrategy<T> applicationStrategy;
    protected final VCAssertiveBlockBuilder enclosingBlock;

    public VCRuleBackedStat(T contents, VCAssertiveBlockBuilder block,
            RuleApplicationStrategy<T> apply) {
        if ( block == null ) {
            throw new IllegalArgumentException("assertive block==null");
        }
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

    public String getApplicationDescription() {
        return applicationStrategy.getDescription();
    }

    public VCAssertiveBlockBuilder getEnclosingBlock() {
        return enclosingBlock;
    }

    public abstract String getText();
}
