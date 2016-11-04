package edu.clemson.resolve.vcgen.application;

import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.semantics.DumbMathClssftnHandler;
import edu.clemson.resolve.vcgen.BasicBetaReducingListener;
import edu.clemson.resolve.vcgen.AssertiveBlock;
import edu.clemson.resolve.vcgen.VCAssertiveBlock;
import edu.clemson.resolve.vcgen.VCAssertiveBlock.VCAssertiveBlockBuilder;
import edu.clemson.resolve.vcgen.stats.VCAssume;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ParsimoniousAssumeApplicationStrategy
        implements
            VCStatRuleApplicationStrategy<VCAssume> {

    @NotNull
    @Override
    public AssertiveBlock applyRule(@NotNull Deque<VCAssertiveBlockBuilder> branches,
                                    @NotNull VCAssertiveBlockBuilder block,
                                    @NotNull VCAssume stat) {
        List<PExp> assumeExpList = stat.getAssumeExp().splitIntoConjuncts();
        List<PExp> confirmExpList = block.finalConfirm.getConfirmExp().splitIntoConjuncts();

        PExp newFinalConfirm = formParsimoniousVC(block.g, confirmExpList, assumeExpList);
        block.finalConfirm(newFinalConfirm);
        return block.snapshot();
    }

    private PExp formParsimoniousVC(DumbMathClssftnHandler g,
                                    List<PExp> assumeExps,
                                    List<PExp> confirmExps) {
        for (PExp confirm : confirmExps) {

        }
    }

    private PExp formImplication(DumbMathClssftnHandler g, PExp confirmExp,
                                 List<PExp> assumeExps,
                                 boolean isStipulatedAssumption) {
        if (isStipulatedAssumption) {
            for (PExp assume : assumeExps) {
                confirmExp = g.formImplies(assume, confirmExp);
            }
            return confirmExp;
        }
        boolean checkList = assumeExps.size() > 0;

        // Loop until we no longer add more expressions or we have added all
        // expressions in the remaining assume expression list.
        while (checkList) {
            List<PExp> tmpExpList = new ArrayList<>();
            boolean formedImplies = false;

            for (PExp assume : assumeExps) {
                // Create a new implies expression if there are common symbols
                // in the assume and in the confirm. (Parsimonious step)
                Set<String> intersection = Utilities.getSymbols(confirmExp);
                intersection.retainAll(Utilities.getSymbols(assumeExp));

                if (!intersection.isEmpty()) {
                    // Don't form implies if we have "Assume true"
                    if (!assumeExp.isLiteralTrue()) {
                        confirmExp =
                                myTypeGraph.formImplies(
                                        Exp.copy(assumeExp), Exp
                                                .copy(confirmExp));
                        formedImplies = true;
                    }
                }
                else {
                    // Form implies if we have "Assume false"
                    if (assumeExp.isLiteralFalse()) {
                        confirmExp =
                                myTypeGraph.formImplies(
                                        Exp.copy(assumeExp), Exp
                                                .copy(confirmExp));
                        formedImplies = true;
                    }
                    else {
                        tmpExpList.add(assumeExp);
                    }
                }
            }

            remAssumeExpList = tmpExpList;
            if (remAssumeExpList.size() > 0) {
                // Check to see if we formed an implication
                if (formedImplies) {
                    // Loop again to see if we can form any more implications
                    checkList = true;
                }
                else {
                    // If no implications are formed, then none of the remaining
                    // expressions will be helpful.
                    checkList = false;
                }
            }
            else {
                // Since we are done with all assume expressions, we can quit
                // out of the loop.
                checkList = false;
            }
        }
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Parsimonious assume application";
    }
}