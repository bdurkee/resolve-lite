package edu.clemson.resolve.vcgen.application;

import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.vcgen.model.AssertiveBlock;
import edu.clemson.resolve.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;

import java.util.List;

public interface StatRuleApplicationStrategy {

    //TODO: maybe instead of statcomponents this should take a VCRuleBackedStat... hmmm.
    public AssertiveBlock applyRule(VCAssertiveBlockBuilder block,
                                    List<PExp> statComponents);

    public AssertiveBlock applyRule(VCAssertiveBlockBuilder block, PExp... e);

    public String getDescription();

}
