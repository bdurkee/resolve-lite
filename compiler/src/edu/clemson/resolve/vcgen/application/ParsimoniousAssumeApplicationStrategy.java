package edu.clemson.resolve.vcgen.application;

import edu.clemson.resolve.misc.Utils;
import edu.clemson.resolve.proving.absyn.PApply;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PSymbol;
import edu.clemson.resolve.vcgen.model.AssertiveBlock;
import edu.clemson.resolve.vcgen.model.VCAssertiveBlock;
import edu.clemson.resolve.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;
import edu.clemson.resolve.vcgen.model.VCAssume;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class ParsimoniousAssumeApplicationStrategy
        implements
            StatRuleApplicationStrategy<VCAssume> {

    //Basically Sami's parsimonious algorithm, but tweaked slightly --
    //probably broke it :)
    @NotNull @Override public AssertiveBlock applyRule(
            @NotNull VCAssertiveBlockBuilder block,
            @NotNull VCAssume stat) {
        PExp assumeExp = stat.getAssumeExp();
        PExp RP = block.finalConfirm.getConfirmExp();

        List<PExp> assumeConjuncts = assumeExp.splitIntoConjuncts();
        List<PExp> confirmConjuncts = RP.splitIntoConjuncts();

        for (PExp confirmConjunct : confirmConjuncts) {
            List<PExp> precAssumes = new LinkedList<>();
            int currentAssumeIndex = 0;
            for (PExp assumeConjunct : assumeConjuncts) {
                if (assumeConjunct.isEquality()) {
                    boolean performedReplacement = false;

                    //note: .get(0) would get the '=' exp, not the first arg
                    PExp lhs = assumeConjunct.getSubExpressions().get(1);
                    PExp rhs = assumeConjunct.getSubExpressions().get(2);
                    if (lhs.isVariable()) {
                        PExp tmp = confirmConjunct.substitute(lhs, rhs);
                        performedReplacement = !tmp.equals(confirmConjunct);

                        //Replace all instances of the left side in
                        //the assume expressions we have already processed.
                        Utils.apply(precAssumes, e -> e.substitute(lhs, rhs));
                        Utils.apply(assumeConjuncts.subList(currentAssumeIndex + 1,
                                assumeConjuncts.size()), e -> e.substitute(lhs, rhs));

                        // Replace all instances of the right side in
                        // the assume expressions we haven't processed.
                        for (int k = currentAssumeIndex + 1; k < assumeConjuncts.size(); k++) {
                            PExp newAssumeExp =  assumeConjuncts.get(k).substitute(lhs, rhs)
                            Utilities.replace(assumeExpCopyList.get(k),
                                    equalsExp.getRight(), equalsExp
                                            .getLeft());
                            assumeExpCopyList.set(k, newAssumeExp);
                        }
                    }


                }
                currentAssumeIndex = currentAssumeIndex + 1;
            }
        }

        return block.snapshot();
    }

    @NotNull @Override public String getDescription() {
        return "parsimonious assume application";
    }
}