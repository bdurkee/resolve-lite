package edu.clemson.resolve.vcgen.application;

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

    @NotNull @Override public AssertiveBlock applyRule(
            @NotNull VCAssertiveBlockBuilder block,
            @NotNull VCAssume stat) {
        PExp assumeExp = stat.getAssumeExp();
        PExp RP = block.finalConfirm.getConfirmExp();
        Set<String> allSymbolNamesAppearingInConfirm = RP.getSymbolNames();

        List<PExp> thingsWeNeedToAssume = new ArrayList<>();
        for (PExp assume : assumeExp.splitIntoConjuncts()) {
            Set<String> curIntersection = assume.getSymbolNames();
            curIntersection.retainAll(allSymbolNamesAppearingInConfirm);

            if (!curIntersection.isEmpty()) {
                thingsWeNeedToAssume.add(assume);
            }
        }
        PExp newFinalConfirm;

        if (!thingsWeNeedToAssume.isEmpty()) {
            PExp prunedAssumes = block.g.formConjuncts(thingsWeNeedToAssume);
            newFinalConfirm = block.g.formImplies(prunedAssumes, RP);
        }
        else {
            newFinalConfirm = RP; //just the RP
        }
        block.finalConfirm(newFinalConfirm);

        //now form implies between prunedAssumes and the final confirm.
       /* Map<PExp, PExp> equalsReplacements = new HashMap<>();
        List<PExp> assumeConjuncts = assumeExp.splitIntoConjuncts();

        List<PExp> relevantAssumptions = new ArrayList<>();
        for (PExp assume : assumeConjuncts) {
            if (assume.isEquality()) {
                PApply assumeAsPSymbol = (PApply)assume;
                PExp left = assumeAsPSymbol.getArguments().get(0);
                PExp right = assumeAsPSymbol.getArguments().get(1);
                if (left.isVariable()) {
                    if (!RP.staysSameAfterSubstitution(left, right)) {
                        equalsReplacements.put(left, right); continue;
                    }
                }
            }
            //if we're an equality that didn't affect RP (see above) then
            if (assume.hasSymbolNamesInCommonWith(RP, true, true) &&
                    !assume.isObviouslyTrue()) {
                relevantAssumptions.add(assume);
            }
        }
        RP = RP.substitute(equalsReplacements);

        PExp newAssume = null;
        if (relevantAssumptions.isEmpty()) {
            block.finalConfirm(RP);
        }
        else {
            newAssume = block.g.formConjuncts(relevantAssumptions);
            block.finalConfirm(block.g.formImplies(newAssume, RP));
        }*/
        return block.snapshot();
    }

    @NotNull
    @Override public String getDescription() {
        return "parsimonious assume application";
    }
}