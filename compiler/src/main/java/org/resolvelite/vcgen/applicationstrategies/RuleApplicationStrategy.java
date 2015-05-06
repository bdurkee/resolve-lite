package org.resolvelite.vcgen.applicationstrategies;

import org.resolvelite.vcgen.vcstat.AssertiveCode;
import org.resolvelite.vcgen.vcstat.VCAssertiveBlock;

public interface RuleApplicationStrategy<T> {

    public void applyRule(T statement, AssertiveCode block);
}
