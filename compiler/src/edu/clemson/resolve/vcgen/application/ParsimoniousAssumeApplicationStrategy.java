package edu.clemson.resolve.vcgen.application;

import edu.clemson.resolve.proving.absyn.PApply;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PSymbol;
import edu.clemson.resolve.vcgen.model.AssertiveBlock;
import edu.clemson.resolve.vcgen.model.VCAssertiveBlock;
import edu.clemson.resolve.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;
import edu.clemson.resolve.vcgen.model.VCAssume;

import java.util.*;
import java.util.stream.Collectors;

public class ParsimoniousAssumeApplicationStrategy
        implements
            StatRuleApplicationStrategy<VCAssume> {

    @Override public AssertiveBlock applyRule(VCAssertiveBlockBuilder block,
                                              VCAssume stat) {
        PExp assumeExp = stat.getAssumeExp();
        PExp confirmExp = block.finalConfirm.getConfirmExp();

        Set<String> allAssumptionSymbolNames = assumeExp.getSymbolNames();
        Map<PExp, Set<String>> assumesToSymbols = new HashMap<>();

        for (PExp assume : assumeExp.splitIntoConjuncts()) {
            assumesToSymbols.put(assume, assume.getSymbolNames());
        }

        for (PExp confirm : confirmExp.splitIntoConjuncts()) {
            Set<String> curIntersection = new HashSet<>();
            confirm.getSymbolNames().retainAll(allAssumptionSymbolNames);

            if (curIntersection.isEmpty()) continue;
            for (PExp assume : assumesToSymbols.keySet()) {
                assumesToSymbols.get(assume).retainAll(curIntersection);
                if (!curIntersection.isEmpty()) {
                    
                }
            }
        }

    /*    Map<PExp, PExp> equalsReplacements = new HashMap<>();
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

    @Override public String getDescription() {
        return "parsimonious assume application";
    }
}