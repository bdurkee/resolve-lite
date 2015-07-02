package edu.clemson.resolve.vcgen.application;

import edu.clemson.resolve.compiler.AnnotatedTree;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.vcgen.ModelBuilderProto;
import edu.clemson.resolve.vcgen.model.AssertiveBlock;
import edu.clemson.resolve.vcgen.model.VCAssertiveBlock;

import java.util.Arrays;
import java.util.List;

public class FunctionAssignApplicationStrategy
        implements
            StatRuleApplicationStrategy {

    @Override public AssertiveBlock applyRule(
            VCAssertiveBlock.VCAssertiveBlockBuilder block,
                                              List<PExp> statComponents) {
        AnnotatedTree annotations = block.annotations;
        PExp leftReplacee = statComponents.get(0);
        PExp rightReplacer = statComponents.get(1);

        if ( rightReplacer.isLiteral() ) {
            PExp workingConfirm = block.finalConfirm.getConfirmExp();
            block.finalConfirm(workingConfirm.substitute(leftReplacee,
                    rightReplacer));
            return block.snapshot();
        }

        //apply explicit call rule to the 'exp-call-like-thing' on the rhs.
        return ModelBuilderProto.EXPLICIT_CALL_APPLICATION.applyRule(block,
                statComponents.get(1));
    }

    @Override public AssertiveBlock applyRule(
            VCAssertiveBlock.VCAssertiveBlockBuilder block, PExp... e) {
        return applyRule(block, Arrays.asList(e));
    }

    @Override public String getDescription() {
        return "function assignment rule application";
    }
}