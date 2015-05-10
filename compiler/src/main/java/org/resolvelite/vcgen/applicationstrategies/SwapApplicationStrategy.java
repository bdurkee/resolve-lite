package org.resolvelite.vcgen.applicationstrategies;

import org.resolvelite.parsing.ResolveParser;
import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.proving.absyn.PSymbol.PSymbolBuilder;
import org.resolvelite.vcgen.vcstat.AssertiveCode;
import org.resolvelite.vcgen.vcstat.VCAssertiveBlock;

public class SwapApplicationStrategy
        implements
            RuleApplicationStrategy<ResolveParser.SwapStmtContext> {

    @Override public AssertiveCode applyRule(
            ResolveParser.SwapStmtContext statement,
            VCAssertiveBlock.VCAssertiveBlockBuilder block) {
        PExp workingConfirm = block.finalConfirm.getContents();
        PExp swapLeft = block.annotations.mathPExps.get(statement.left);
        PExp swapRight = block.annotations.mathPExps.get(statement.right);

        PExp temp =
                new PSymbolBuilder("_t").mathType(swapLeft.getMathType())
                        .build();

        workingConfirm = workingConfirm.substitute(swapRight, temp);
        workingConfirm = workingConfirm.substitute(swapLeft, swapRight);
        workingConfirm = workingConfirm.substitute(temp, swapLeft);
        block.finalConfirm(workingConfirm);
        System.out.println("swap rule applied to: " + statement.getText());
        return block.snapshot();
    }
}
