package edu.clemson.resolve.vcgen.application;

import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PSymbol;
import edu.clemson.resolve.vcgen.model.AssertiveBlock;
import edu.clemson.resolve.vcgen.model.VCAssertiveBlock;

import java.util.*;
import java.util.stream.Collectors;

public class FrugalAssumeApplicationStrategy
        implements
            StatRuleApplicationStrategy {

    @Override public AssertiveBlock applyRule(
            VCAssertiveBlock.VCAssertiveBlockBuilder block, PExp... e) {
        return applyRule(block, Arrays.asList(e));
    }

    @Override public AssertiveBlock applyRule(
            VCAssertiveBlock.VCAssertiveBlockBuilder block,
            List<PExp> statComponents) {
        PExp assumeExp = statComponents.get(0);
        PExp finalConfirmExp = block.finalConfirm.getConfirmExp();
        Map<PExp, PExp> equalsReplacements = new HashMap<>();
        List<PExp> assumeConjuncts = assumeExp.splitIntoConjuncts();
        for (PExp assume : assumeConjuncts) {
            if (assume instanceof PSymbol &&
                    ((PSymbol) assume).getName().equals("=")) {
                PSymbol assumeAsPSymbol = (PSymbol)assume;
                PExp left = assumeAsPSymbol.getArguments().get(0);
                PExp right = assumeAsPSymbol.getArguments().get(1);
                //we don't do replacements when the lhs and rhs are reg. vars.
                if (left.isVariable() && right.isVariable()) {
                    //TODO: Do strange, dirty things with P_Val and Cum_Dur.
                }
                else if (left.isVariable()) {
                    equalsReplacements.put(left, right);
                }
            }
        }
        //assumeExp = assumeExp.substitute(equalsReplacements);
        finalConfirmExp = finalConfirmExp.substitute(equalsReplacements);
        Set<String> confirmSymNames = finalConfirmExp.getSymbolNames(true);

        List<PExp> relevantUntouchedAssumptions = assumeConjuncts.stream()
                .filter(a -> a.staysSameAfterSubstitution(equalsReplacements))
                .filter(a -> sharesNamesWithConfirm(a, confirmSymNames))
                .filter(a -> !a.isObviouslyTrue())
                .collect(Collectors.toList());

        List<PExp> relevantTouchedAssumptions = assumeConjuncts.stream()
                .filter(a -> !a.staysSameAfterSubstitution(equalsReplacements))
                .map(b -> b.substitute(equalsReplacements))
                .filter(c -> !c.isObviouslyTrue())
                .collect(Collectors.toList());

        relevantUntouchedAssumptions.addAll(relevantTouchedAssumptions);
        if (relevantUntouchedAssumptions.isEmpty()) {
            block.finalConfirm(finalConfirmExp);
        }
        else {
            PExp newConfirm =
                    block.g.formImplies(
                            block.g.formConjuncts(relevantUntouchedAssumptions),
                            finalConfirmExp);
            block.finalConfirm(newConfirm);
        }
        return block.snapshot();
    }

    private boolean sharesNamesWithConfirm(PExp assume,
                                           Set<String> confirmSyms) {
        Set<String> assumeNames = assume.getSymbolNames(true);
        assumeNames.retainAll(confirmSyms);
        return !assumeNames.isEmpty();
    }

    @Override public String getDescription() {
        return "parsimonious assume application";
    }
}
