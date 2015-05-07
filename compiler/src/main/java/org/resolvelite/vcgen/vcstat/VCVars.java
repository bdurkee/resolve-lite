package org.resolvelite.vcgen.vcstat;

import org.antlr.v4.runtime.misc.NotNull;
import org.resolvelite.parsing.ResolveParser;
import org.resolvelite.vcgen.applicationstrategies.RuleApplicationStrategy;

public class VCVars
        extends
            VCRuleTargetedStat<ResolveParser.VariableDeclGroupContext> {
    public VCVars(
            ResolveParser.VariableDeclGroupContext contents,
            RuleApplicationStrategy<ResolveParser.VariableDeclGroupContext> apply,
            AssertiveCode enclosingBlock) {
        super(contents, enclosingBlock, apply);
    }

    public VCVars(ResolveParser.VariableDeclGroupContext contents,
            AssertiveCode enclosingBlock) {
        super(contents, enclosingBlock, null);
    }
}