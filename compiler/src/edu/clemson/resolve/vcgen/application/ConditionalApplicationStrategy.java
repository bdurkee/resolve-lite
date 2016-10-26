package edu.clemson.resolve.vcgen.application;

import edu.clemson.resolve.parser.ResolveParser;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.vcgen.model.*;
import org.jetbrains.annotations.NotNull;

public abstract class ConditionalApplicationStrategy implements VCStatRuleApplicationStrategy<VCIfElse> {

    public static class IfApplicationStrategy extends ConditionalApplicationStrategy {

        @NotNull
        @Override
        public AssertiveBlock applyRule(@NotNull VCAssertiveBlock.VCAssertiveBlockBuilder block,
                                        @NotNull VCIfElse stat) {
            //get the math exp form of the 'if condition'...
            PExp progIfCondition = stat.getIfCondition();
            ResolveParser.IfStmtContext ifCtx = (ResolveParser.IfStmtContext) stat.getDefiningContext();
            FunctionAssignApplicationStrategy.Invk_Cond invokeConditionListener =
                    new FunctionAssignApplicationStrategy.Invk_Cond(ifCtx.progExp(), block);
            progIfCondition.accept(invokeConditionListener);
            PExp mathCond = invokeConditionListener.mathFor(progIfCondition);

            block.assume(mathCond);
            block.stats(stat.getThenStmts());
            return block.snapshot();
        }

        @NotNull
        @Override
        public String getDescription() {
            return "If rule application";
        }
    }

    public static class ElseApplicationStrategy extends ConditionalApplicationStrategy {

        @NotNull
        @Override
        public AssertiveBlock applyRule(@NotNull VCAssertiveBlock.VCAssertiveBlockBuilder block,
                                        @NotNull VCIfElse stat) {
            //get the netated math exp form of the 'if condition'...
            PExp negatedCondition = stat.negateMathCondition(getMathCondition(block, stat));
            block.assume(negatedCondition);
            block.stats(stat.getElseStmts());
            return block.snapshot();
        }

        @NotNull
        @Override
        public String getDescription() {
            return "Negated if rule application";
        }
    }

    @NotNull
    PExp getMathCondition(@NotNull VCAssertiveBlock.VCAssertiveBlockBuilder block,
                          @NotNull VCIfElse stat) {
        PExp progCondition = stat.getIfCondition();
        ResolveParser.IfStmtContext ifCtx = (ResolveParser.IfStmtContext) stat.getDefiningContext();
        FunctionAssignApplicationStrategy.Invk_Cond invokeConditionListener =
                new FunctionAssignApplicationStrategy.Invk_Cond(ifCtx.progExp(), block);
        progCondition.accept(invokeConditionListener);
        PExp mathCond = invokeConditionListener.mathFor(progCondition);
        return mathCond;
    }
}
