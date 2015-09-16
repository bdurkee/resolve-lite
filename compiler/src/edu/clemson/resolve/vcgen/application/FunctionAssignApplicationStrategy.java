package edu.clemson.resolve.vcgen.application;

import edu.clemson.resolve.compiler.AnnotatedTree;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PSymbol;
import edu.clemson.resolve.vcgen.ModelBuilderProto;
import edu.clemson.resolve.vcgen.model.AssertiveBlock;
import edu.clemson.resolve.vcgen.model.VCAssertiveBlock;
import edu.clemson.resolve.vcgen.model.VCRuleBackedStat;
import org.rsrg.semantics.symbol.OperationSymbol;
import org.rsrg.semantics.symbol.ProgParameterSymbol;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class FunctionAssignApplicationStrategy
        implements
            StatRuleApplicationStrategy<VCRuleBackedStat> {

    @Override public AssertiveBlock applyRule(
            VCAssertiveBlock.VCAssertiveBlockBuilder block,
            VCRuleBackedStat stat) {
        AnnotatedTree annotations = block.annotations;
        PExp leftReplacee = stat.getStatComponents().get(0);
        PExp rightReplacer = stat.getStatComponents().get(1);

        if ( !(rightReplacer.isFunctionApplication()) ) {
            PExp workingConfirm = block.finalConfirm.getConfirmExp();
            block.finalConfirm(workingConfirm.substitute(leftReplacee,
                    rightReplacer));
            return block.snapshot();
        }
        //we know rightReplacer is a function applications (see above)
        PExp modifiedEnsures =
                ExplicitCallApplicationStrategy.modifyExplicitCallEnsures(block,
                        (PSymbol)rightReplacer);
        if (modifiedEnsures.equals(block.g.getTrueExp())) return block.snapshot();

        block.finalConfirm(block.finalConfirm.getConfirmExp()
                .substitute(leftReplacee, modifiedEnsures));
        return block.snapshot();
    }

    @Override public String getDescription() {
        return "function assignment rule application";
    }
}