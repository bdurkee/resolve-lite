package org.resolvelite.vcgen.application;

import org.resolvelite.compiler.tree.AnnotatedTree;
import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.vcgen.ModelBuilderProto;
import org.resolvelite.vcgen.model.AssertiveBlock;
import org.resolvelite.vcgen.model.VCAssertiveBlock;
import org.resolvelite.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;

import java.util.Arrays;
import java.util.List;

public class FunctionAssignApplicationStrategy
        implements
        StatRuleApplicationStrategy {

    @Override public AssertiveBlock applyRule(VCAssertiveBlockBuilder block,
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
