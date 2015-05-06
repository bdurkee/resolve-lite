package org.resolvelite.vcgen.applicationstrategies;

import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.vcgen.applicationstrategies.RuleApplicationStrategy;
import org.resolvelite.vcgen.vcstat.AssertiveCode;
import org.resolvelite.vcgen.vcstat.VCAssertiveBlock;

public class AssumeApplicationStrategy implements RuleApplicationStrategy<PExp> {

    @Override public void applyRule(PExp statement, AssertiveCode block) {}
}
