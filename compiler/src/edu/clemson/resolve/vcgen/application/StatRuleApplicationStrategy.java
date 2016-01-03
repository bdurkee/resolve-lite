package edu.clemson.resolve.vcgen.application;

import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.vcgen.model.AssertiveBlock;
import edu.clemson.resolve.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;
import edu.clemson.resolve.vcgen.model.VCRuleBackedStat;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface StatRuleApplicationStrategy<T extends VCRuleBackedStat> {

    @NotNull public AssertiveBlock applyRule(
            @NotNull VCAssertiveBlockBuilder block,
            @NotNull T stat);

    @NotNull public String getDescription();
}
