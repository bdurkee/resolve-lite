package edu.clemson.resolve.vcgen.application;

import edu.clemson.resolve.parser.ResolveParser;
import edu.clemson.resolve.vcgen.AssertiveBlock;
import edu.clemson.resolve.vcgen.VCAssertiveBlock;
import edu.clemson.resolve.vcgen.stats.VCConfirm;
import edu.clemson.resolve.vcgen.VCAssertiveBlock.VCAssertiveBlockBuilder;
import edu.clemson.resolve.vcgen.stats.VCWhile;
import org.antlr.v4.runtime.ParserRuleContext;
import org.jetbrains.annotations.NotNull;

public class WhileApplicationStrategy implements VCStatRuleApplicationStrategy<VCWhile> {

    @NotNull
    @Override
    public AssertiveBlock applyRule(@NotNull VCAssertiveBlockBuilder block, @NotNull VCWhile stat) {
        ResolveParser.WhileStmtContext whileNode = (ResolveParser.WhileStmtContext) stat.getDefiningContext();
        //I *require* a maintaining clause on all loops..
        //TODO: Look into this crap where the confirm needs a ctx...maybe it's needed.. can't remember..
        block.confirm(whileNode.maintainingClause(),
                stat.getInvariant().withVCInfo(whileNode.maintainingClause().getStart(),
                        "Base case of while loop invariant"));


        return block.snapshot();
    }

    @NotNull
    @Override
    public String getDescription() {
        return "While rule application";
    }
}
