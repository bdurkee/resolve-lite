package org.resolvelite.vcgen.vcstat;

import org.antlr.v4.runtime.misc.NotNull;
import org.resolvelite.codegen.model.OutputModelObject;
import org.resolvelite.vcgen.applicationstrategies.RuleApplicationStrategy;

public abstract class VCRuleTargetedStat<T> extends OutputModelObject {

    private final T contents;
    private final RuleApplicationStrategy<T> applicationStrategy;
    private final AssertiveCode enclosingBlock;

    public VCRuleTargetedStat(@NotNull T contents,
            @NotNull AssertiveCode block,
            @NotNull RuleApplicationStrategy<T> apply) {
        this.contents = contents;
        this.applicationStrategy = apply;
        this.enclosingBlock = block;
    }

    public void reduce() {
        applicationStrategy.applyRule(contents, enclosingBlock);
    }

    public T getAssertion() {
        return contents;
    }

    public AssertiveCode getEnclosingBlock() {
        return enclosingBlock;
    }
}
