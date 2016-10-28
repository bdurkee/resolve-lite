package edu.clemson.resolve.vcgen.application;

import edu.clemson.resolve.vcgen.AssertiveBlock;
import edu.clemson.resolve.vcgen.VCAssertiveBlock.VCAssertiveBlockBuilder;
import edu.clemson.resolve.vcgen.stats.VCRuleBackedStat;
import org.jetbrains.annotations.NotNull;

public interface VCStatRuleApplicationStrategy<T extends VCRuleBackedStat> {

    @NotNull
    public AssertiveBlock applyRule(@NotNull VCAssertiveBlockBuilder block, @NotNull T stat);

    @NotNull
    public String getDescription();
}
