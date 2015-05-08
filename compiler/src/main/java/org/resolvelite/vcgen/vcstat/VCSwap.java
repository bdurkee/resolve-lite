package org.resolvelite.vcgen.vcstat;

import org.resolvelite.parsing.ResolveParser;
import org.resolvelite.vcgen.applicationstrategies.RuleApplicationStrategy;

public class VCSwap extends VCRuleTargetedStat<ResolveParser.SwapStmtContext> {

    public VCSwap(ResolveParser.SwapStmtContext contents, AssertiveCode block,
                  RuleApplicationStrategy<ResolveParser.SwapStmtContext> apply) {
        super(contents, block, apply);
    }
}
