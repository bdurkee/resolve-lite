package org.resolvelite.vcgen.model;

import org.resolvelite.codegen.model.OutputModelObject;
import org.resolvelite.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;
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

    /**
     * Applies the provided {@link RuleApplicationStrategy} to this statement
     * and returns an immutable view of the current {@link AssertiveCode}
     * proceeding its application.
     *
     * @return
     */
    public AssertiveCode reduce() {
        return applicationStrategy.applyRule(contents, enclosingBlock);
    }

    public abstract String getText();

    public T getContents() {
        return contents;
    }

    public String getApplicationDescription() {
        return applicationStrategy.getDescription();
    }

    public VCAssertiveBlockBuilder getEnclosingBlock() {
        return enclosingBlock;
    }
}
