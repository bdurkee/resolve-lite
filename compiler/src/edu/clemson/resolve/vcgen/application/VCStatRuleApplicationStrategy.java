package edu.clemson.resolve.vcgen.application;

import edu.clemson.resolve.vcgen.model.AssertiveBlock;
import edu.clemson.resolve.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;
import edu.clemson.resolve.vcgen.model.VCRuleBackedStat;
import org.antlr.v4.runtime.ParserRuleContext;
import org.jetbrains.annotations.NotNull;

public interface VCStatRuleApplicationStrategy<T extends VCRuleBackedStat> {

    @NotNull
    public AssertiveBlock applyRule(@NotNull VCAssertiveBlockBuilder block, @NotNull T stat);

    @NotNull
    public String getDescription();


}
