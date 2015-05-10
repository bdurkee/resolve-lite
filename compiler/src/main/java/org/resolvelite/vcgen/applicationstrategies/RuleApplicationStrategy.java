package org.resolvelite.vcgen.applicationstrategies;

import org.resolvelite.vcgen.vcstat.AssertiveCode;
import org.resolvelite.vcgen.vcstat.VCAssertiveBlock.VCAssertiveBlockBuilder;

public interface RuleApplicationStrategy<T> {

    //Todo: Maybe this should return AssertiveCode so in 'build' we can print
    //everything with the model converter?
    public AssertiveCode applyRule(T statement, VCAssertiveBlockBuilder block);
}
