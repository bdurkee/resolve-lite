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
        //the short story: you don't want to discard the conjuncts that formed
        //an equals relationship (and you always are assured to do so if you
        //just perform the substitution on the entire expression -- as I currently am)
        PExp assumeExp = stat.getAssumeExp();
        PExp RP = block.finalConfirm.getConfirmExp();

        Map<PExp, PExp> equalsReplacements = new HashMap<>();
        List<PExp> assumeConjuncts = assumeExp.splitIntoConjuncts();

        List<PExp> relevantAssumptions = new ArrayList<>();
        for (PExp assume : assumeConjuncts) {
            if (assume.isEquality()) {
                PApply assumeAsPSymbol = (PApply)assume;
                PExp left = assumeAsPSymbol.getArguments().get(0);
                PExp right = assumeAsPSymbol.getArguments().get(1);
                if (left.isVariable()) {
                    //if this assumed equality affects something in RP, then
                    //we skip the rest of the logic of this loop since we now
                    //know this assume is important (doesn't matter anymore if
                    // it has (other) symbols in common w/ RP)
                    if (!RP.staysSameAfterSubstitution(left, right)) {
                        equalsReplacements.put(left, right); continue;
                    }
                    //the one I'm hung up on stays because its equality didn't affect RP and it had symbols in common!
                    //if the substitution from that clause affects RP, it gets removed (even if it has symbols in common)
                }
            }
            //if we're an equality that didn't affect RP (see above) then
            if (assume.hasSymbolNamesInCommonWith(RP, true, true) &&
                    !assume.isObviouslyTrue()) {
                relevantAssumptions.add(assume);
            }
        }


      /*  for (PExp assume : assumeConjuncts) {
            if (assume.isEquality()) {
                PApply assumeAsPSymbol = (PApply)assume;
                PExp left = assumeAsPSymbol.getArguments().get(0);
                PExp right = assumeAsPSymbol.getArguments().get(1);
                if (left.isVariable()) {
                    equalsReplacements.put(left, right);
                }
            }
        }
        //substitute all found equivalences into the confirm

        //now find conjuncts in the assume that are relevant to the confirm
        final PExp finalRP = RP;
        List<PExp> relevantAssumptions = assumeConjuncts.stream()
                .filter(PExp::isNotObviouslyTrue)
                .filter(e -> e.hasSymbolNamesInCommonWith(finalRP, true, true))
                .collect(Collectors.toList());
*/
        RP = RP.substitute(equalsReplacements);

        PExp newAssume = null;
        if (relevantAssumptions.isEmpty()) {
            block.finalConfirm(RP);
        }
        else {
            newAssume = block.g.formConjuncts(relevantAssumptions);
            block.finalConfirm(block.g.formImplies(newAssume, RP));
        }
        return block.snapshot();
    }

    //Older version of this method (as of 10-15)
    /* @Override public AssertiveBlock applyRule(
            VCAssertiveBlock.VCAssertiveBlockBuilder block, VCAssume stat) {

        PExp assumeExp = stat.getAssumeExp();
        PExp RP = block.finalConfirm.getConfirmExp();
        Map<PExp, PExp> equalsReplacements = new HashMap<>();
        List<PExp> assumeConjuncts = assumeExp.splitIntoConjuncts();
        for (PExp assume : assumeConjuncts) {
            if (assume.isEquality()) {
                PApply assumeAsPSymbol = (PApply)assume;
                PExp left = assumeAsPSymbol.getArguments().get(0);
                PExp right = assumeAsPSymbol.getArguments().get(1);
                //we don't do replacements when the lhs and rhs are reg. vars.
                if (right.isVariable() && left.isVariable()) {
                    //TODO: Do strange, dirty things with P_Val and Cum_Dur.
                }
                if (left.isVariable()) {
                    equalsReplacements.put(left, right);
                }
            }
        }
        //idea -- pass in the original expr
        //assumeExp = assumeExp.substitute(equalsReplacements);
        finalConfirmExp = finalConfirmExp.substitute(equalsReplacements);
        //(true,true) excludes function applications and literals, respectively
        Set<String> confirmSymNames = finalConfirmExp.getSymbolNames(true, true);
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
        //TODO: I'm not sure about this line... definitely seems counter intuitive
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
    }*/

    private boolean sharesNamesWithConfirm(PExp assume,
                                           Set<String> confirmSyms) {
        Set<String> assumeNames = assume.getSymbolNames();
        assumeNames.retainAll(confirmSyms);
        return !assumeNames.isEmpty();
    }

    @Override public String getDescription() {
        return "parsimonious assume application";
    }
}