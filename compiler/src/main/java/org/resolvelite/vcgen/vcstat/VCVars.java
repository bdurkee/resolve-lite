package org.resolvelite.vcgen.vcstat;

import org.antlr.v4.runtime.misc.NotNull;
import org.resolvelite.parsing.ResolveParser;
import org.resolvelite.vcgen.applicationstrategies.RuleApplicationStrategy;

public class VCVars
        extends
            VCRuleTargetedStat<ResolveParser.VariableDeclGroupContext> {
    public VCVars(
            @NotNull ResolveParser.VariableDeclGroupContext contents,
            @NotNull RuleApplicationStrategy<ResolveParser.VariableDeclGroupContext> apply) {
        super(contents, apply);
    }
}