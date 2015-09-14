package edu.clemson.resolve.vcgen.application;

import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.vcgen.model.AssertiveBlock;
import edu.clemson.resolve.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;
import edu.clemson.resolve.vcgen.model.VCRuleBackedStat;

import java.util.List;

public interface StatRuleApplicationStrategy<T extends VCRuleBackedStat> {

    //TODO: Honestly, we probably could get away by just passing the VCRuleBackedStat (it already contains the enclosing block)
    public AssertiveBlock applyRule(VCAssertiveBlockBuilder block, T stat);

    public String getDescription();

}
