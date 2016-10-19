package edu.clemson.resolve.vcgen.application;

import edu.clemson.resolve.parser.ResolveParser;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.vcgen.model.*;
import org.jetbrains.annotations.NotNull;

public class IfElseApplicationStrategy implements VCStatRuleApplicationStrategy<VCIfElse> {

    @NotNull
    @Override
    public AssertiveBlock applyRule(@NotNull VCAssertiveBlock.VCAssertiveBlockBuilder block, @NotNull VCIfElse stat) {

        //get the math exp form of the 'if condition'...
        PExp progIfCondition = stat.getIfCondition();
        ResolveParser.IfStmtContext ifCtx = (ResolveParser.IfStmtContext) stat.getDefiningContext();
        FunctionAssignApplicationStrategy.Invk_Cond invokeConditionListener =
                new FunctionAssignApplicationStrategy.Invk_Cond(ifCtx.progExp(), block);
        progIfCondition.accept(invokeConditionListener);
        PExp mathCond = invokeConditionListener.mathFor(progIfCondition);

        if (stat.isForIf()) {
            block.assume(mathCond);
            block.stats(stat.getBodyStatements());
        }
        else {
            PExp negatedCondition = stat.negateMathCondition(mathCond);
            block.assume(negatedCondition);
            //block.stats(stat.getBodyStatements());
        }

        return block.snapshot();
    }

    @NotNull
    @Override
    public String getDescription() {
        return "If rule application";
    }
}
