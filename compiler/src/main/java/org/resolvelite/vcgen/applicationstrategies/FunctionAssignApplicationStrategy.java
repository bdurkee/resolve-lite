package org.resolvelite.vcgen.applicationstrategies;

import org.antlr.v4.runtime.ParserRuleContext;
import org.resolvelite.parsing.ResolveParser;
import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.vcgen.model.AssertiveCode;
import org.resolvelite.vcgen.model.VCAssertiveBlock;

public class FunctionAssignApplicationStrategy
        implements
            RuleApplicationStrategy<ResolveParser.AssignStmtContext> {

    @Override public AssertiveCode applyRule(
            ResolveParser.AssignStmtContext statement,
            VCAssertiveBlock.VCAssertiveBlockBuilder block) {
        PExp leftReplacee =
                block.annotations.mathPExps.get(statement.left);
        PExp rightReplacer =
                block.annotations.mathPExps.get(statement.right);
        if (rightReplacer.isLiteral()) {
            PExp workingConfirm = block.finalConfirm.getContents();
            block.finalConfirm(workingConfirm
                    .substitute(leftReplacee, rightReplacer));
            return block.snapshot();
        }
        //Todo: else if (rightReplace.isDot...)
        if
        System.out.println("RIGHT payload: " + statement.right.getPayload().getText());
        if (statement.right.getPayload() instanceof ResolveParser.ProgParamExpContext) {
            System.out.println("HERERERE: " +statement.right.getPayload().getClass());
        }

       // block.scope.queryForOne(new OperationQue)
        return block.snapshot();
    }

    @Override public String getDescription() {
        return "function assignment rule application";
    }
}
