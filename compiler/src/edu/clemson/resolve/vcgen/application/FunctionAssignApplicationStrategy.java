package edu.clemson.resolve.vcgen.application;

import edu.clemson.resolve.compiler.AnnotatedTree;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.vcgen.ModelBuilderProto;
import edu.clemson.resolve.vcgen.model.AssertiveBlock;
import edu.clemson.resolve.vcgen.model.VCAssertiveBlock;
import edu.clemson.resolve.vcgen.model.VCRuleBackedStat;

import java.util.Arrays;
import java.util.List;

public class FunctionAssignApplicationStrategy
        implements
            StatRuleApplicationStrategy<VCRuleBackedStat> {

    @Override public AssertiveBlock applyRule(
            VCAssertiveBlock.VCAssertiveBlockBuilder block,
            VCRuleBackedStat stat) {
        AnnotatedTree annotations = block.annotations;
        PExp leftReplacee = stat.getStatComponents().get(0);
        PExp rightReplacer = stat.getStatComponents().get(1);

        if ( rightReplacer.isLiteral() || !(rightReplacer.isFunctionApplication()) ) {
            PExp workingConfirm = block.finalConfirm.getConfirmExp();
            block.finalConfirm(workingConfirm.substitute(leftReplacee,
                    rightReplacer));
            return block.snapshot();
        }

        //apply explicit call rule to the 'exp-call-like-thing' on the rhs.
        return ModelBuilderProto.EXPLICIT_CALL_APPLICATION.applyRule(block,
                stat);
    }

    @Override public String getDescription() {
        return "function assignment rule application";
    }
}