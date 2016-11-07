package edu.clemson.resolve.vcgen.application;

import edu.clemson.resolve.misc.Utils;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.semantics.DumbMathClssftnHandler;
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
    public VCAssertiveBlock applyRule(@NotNull Deque<VCAssertiveBlockBuilder> branches,
                                      @NotNull VCAssertiveBlockBuilder block,
                                      @NotNull VCAssume stat) {
        List<PExp> allAssumptions = stat.getAssumeExp().splitIntoConjuncts();
        Map<PExp, PExp> equalitySubstitutions = new HashMap<>();
        PExp existingConfirm = block.finalConfirm.getConfirmExp();
        List<PExp> remainingAssumptions = new ArrayList<>();
        List<PExp> nonEffectualEqualities = new ArrayList<>();

        for (PExp assume : allAssumptions) {
            if (stat.isStipulatedAssumption()) {
                remainingAssumptions.add(assume);
                continue;
            }
            if (assume.isEquality()) {
                PExp lhs = assume.getSubExpressions().get(1);
                PExp rhs = assume.getSubExpressions().get(2);

                //if both lhs and rhs are replaceable vars, then the left had better
                //be a special verification-system conjured variable
                if (lhs.isVariable() && rhs.isVariable()) {
                    boolean hasVerificationVar = lhs.isVariable() &&
                            (lhs.getTopLevelOperationName().contains("P_Val") ||
                             lhs.getTopLevelOperationName().contains("conc"));
                    if (hasVerificationVar) {
                        equalitySubstitutions.put(lhs, rhs);
                    }
                    else {
                        remainingAssumptions.add(assume);
                    }
                }
                //left replaceablility
                else if (lhs.isVariable()) {
                    equalitySubstitutions.put(lhs, rhs);
                    //if we didn't do a replacement, then add it
                    if (existingConfirm.substitute(lhs, rhs).equals(existingConfirm)) {
                        nonEffectualEqualities.add(assume);
                    }
                }
                //right replaceability
                else if (rhs.isVariable()) {
                    equalitySubstitutions.put(rhs, lhs);
                    if (existingConfirm.substitute(rhs, lhs).equals(existingConfirm)) {
                        nonEffectualEqualities.add(assume);
                    }
                }
                //not replaceable...
                else {
                    remainingAssumptions.add(assume);
                }
            }
            else {
                //if we haven't done a replacement, then add it as well..
                remainingAssumptions.add(assume);
            }
        }
        List<PExp> remainingAssumptionsWithEqualSubt = new ArrayList<>();
        for (PExp assumption : remainingAssumptions) {
            remainingAssumptionsWithEqualSubt.add(assumption.substitute(equalitySubstitutions));
        }
        remainingAssumptionsWithEqualSubt.addAll(nonEffectualEqualities);

        PExp substitutedConfirm = block.finalConfirm.getConfirmExp()
                .substitute(equalitySubstitutions);
        PExp newFinalConfirm = performParsimoniousStep(block.g, remainingAssumptionsWithEqualSubt,
                substitutedConfirm, stat.isStipulatedAssumption());
        block.finalConfirm(newFinalConfirm);
        return block.snapshot();
    }

    private PExp performParsimoniousStep(DumbMathClssftnHandler g,
                                         List<PExp> assumptions,
                                         PExp existingConfirm, boolean stipulated) {
        Map<PExp, PExp> confirmsToModifiedConfirms = new LinkedHashMap<>();
        List<PExp> confirms = existingConfirm.splitIntoConjuncts();
        confirmsToModifiedConfirms = Utils.zip(confirms, confirms);

        for (PExp assume : assumptions) {
            for (PExp confirm : confirms) {
                Set<String> intersection = assume.getSymbolNames(true, true);
                intersection.retainAll(confirm.getSymbolNames(true, true));
                if ((!intersection.isEmpty() && !assume.isObviouslyTrue()) || stipulated) {
                    PExp existing = confirmsToModifiedConfirms.get(confirm);
                    confirmsToModifiedConfirms.put(confirm, g.formImplies(assume, existing));
                }
            }
        }
        List<PExp> result = new ArrayList<>(confirmsToModifiedConfirms.values());
        return g.formConjuncts(result);
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Parsimonious assume application";
    }
}