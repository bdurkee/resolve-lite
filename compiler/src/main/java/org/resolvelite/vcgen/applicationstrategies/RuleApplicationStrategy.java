package org.resolvelite.vcgen.applicationstrategies;

import org.resolvelite.vcgen.model.AssertiveCode;
import org.resolvelite.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;

public interface RuleApplicationStrategy<T> {

    public AssertiveCode applyRule(T statement, VCAssertiveBlockBuilder block);

    public String getDescription();

}
