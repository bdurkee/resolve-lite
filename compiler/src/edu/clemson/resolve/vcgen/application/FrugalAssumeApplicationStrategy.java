package edu.clemson.resolve.vcgen.application;

import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PSymbol;
import edu.clemson.resolve.vcgen.model.AssertiveBlock;
import edu.clemson.resolve.vcgen.model.VCAssertiveBlock;

import java.util.*;

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
        List<PExp> assumeConjuncts = assumeExp.splitIntoConjuncts();
        Map<PExp, PExp> equalsReplacements = new HashMap<>();

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

      /*  Set<String> preSubstitutionAssumeNameOccurences =
                assumeExp.getSymbolNames();
        Set<String> preSubstitutionConfirmNameOccurences =
                finalConfirmExp.getSymbolNames();
        Set<String> preSubstitutionNamesIntersection =
                new HashSet<>(preSubstitutionAssumeNameOccurences);

        preSubstitutionNamesIntersection
                .retainAll(preSubstitutionConfirmNameOccurences);*/

        //before we do these substitutions, let's actually do the variable
        //occurrence step, then replace the
        assumeExp = assumeExp.substitute(equalsReplacements);
        finalConfirmExp = finalConfirmExp.substitute(equalsReplacements);

        for (PExp assume : assumeConjuncts) {
            Set<String> curAssumePieceNames = assume.getSymbolNames();
            //if a variable in one of our assume-conjuncts makes an appearance
            //(anywhere!.. right?) in the final confirm expression
            //(and it's not in the valueset of our replacement mappings)
            //then we need to form an implication that reads assume => finalConfirm
            if (finalConfirmExp.containsAtLeastOneOf(curAssumePieceNames)) { // AND

            }
            //if (assume.)
        }
        //PExp currentFinalConfirm =
        //        formParsimoniousVC(confirmExpList, assumeExpList, stmt
        //                .getIsStipulate());
        return block.snapshot();
    }

    @Override public String getDescription() {
        return "parsimonious assume application";
    }
}
