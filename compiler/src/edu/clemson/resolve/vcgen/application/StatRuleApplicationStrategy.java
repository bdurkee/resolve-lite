package edu.clemson.resolve.vcgen.application;

import java.util.List;

public interface StatRuleApplicationStrategy {

    public AssertiveBlock applyRule(VCAssertiveBlockBuilder block,
                                    List<PExp> statComponents);

    public AssertiveBlock applyRule(VCAssertiveBlockBuilder block, PExp... e);

    public String getDescription();

}
