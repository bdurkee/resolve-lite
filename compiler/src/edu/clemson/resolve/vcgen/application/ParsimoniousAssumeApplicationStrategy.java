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
import org.rsrg.semantics.TypeGraph;

import java.util.*;
import java.util.stream.Collectors;

public class ParsimoniousAssumeApplicationStrategy
        implements
            StatRuleApplicationStrategy<VCAssume> {

    //Basically Sami's parsimonious algorithm, but tweaked slightly --
    //probably broke it
    @NotNull @Override public AssertiveBlock applyRule(
            @NotNull VCAssertiveBlockBuilder block,
            @NotNull VCAssume stat) {
        PExp assumeExp = stat.getAssumeExp();
        PExp RP = block.finalConfirm.getConfirmExp();

        List<PExp> assumeConjuncts = assumeExp.splitIntoConjuncts();
        List<PExp> newConfirmConjuncts = new LinkedList<>();

        for (PExp confirmConjunct : RP.splitIntoConjuncts()) {
            List<PExp> precAssumes = new LinkedList<>();
            PExp currentConfirmConjunct = confirmConjunct;
            int currentAssumeIndex = 0;
            for (PExp assumeConjunct : assumeConjuncts) {
                PExp temp = currentConfirmConjunct;
                if (assumeConjunct.isEquality()) {
                    boolean performedReplacement = false;

                    //note: .get(0) would get the '=' exp, not the first arg
                    PExp lhs = assumeConjunct.getSubExpressions().get(1);
                    PExp rhs = assumeConjunct.getSubExpressions().get(2);
                    if (lhs.isVariable()) {
                        temp = confirmConjunct.substitute(lhs, rhs);
                        performedReplacement = !temp.equals(confirmConjunct);

                        //Replace all instances of the left side in
                        //the assume expressions we have already processed.
                        Utils.apply(precAssumes, e -> e.substitute(lhs, rhs));

                        //Replace all instances of the right side in
                        //the assume expressions we haven't processed.
                        for (int k = currentAssumeIndex + 1; k < assumeConjuncts.size(); k++) {
                            PExp newAssumeExp =
                                    assumeConjuncts.get(k).substitute(lhs, rhs);
                            assumeConjuncts.set(k, newAssumeExp);
                        }
                    }

                    // Check to see if this is a stipulate assume clause
                    // If yes, we keep a copy of the current
                    // assume expression.
                    //TODO TODO
                   // if (isStipulate) {
                   //     remAssumeExpList.add(Exp.copy(currentAssumeExp));
                   // }
                   // else {
                        // Update the current confirm expression
                        // if we did a replacement.
                    if (performedReplacement) {
                        currentConfirmConjunct = temp;
                    }
                    else {
                        // Check to see if this a verification
                        // variable. If yes, we don't keep this assume.
                        // Otherwise, we need to store this for the
                        // step that generates the parsimonious vcs.
                        precAssumes.add(currentConfirmConjunct);
                    }
                }
                currentAssumeIndex = currentAssumeIndex + 1;
            }
            PExp newConfirmExp =
                    formImplies(block.g, confirmConjunct, precAssumes);
            newConfirmConjuncts.add(newConfirmExp);
        }
        return block.snapshot();
    }

    private PExp formImplies(TypeGraph g, PExp currentConfirm,
                             List<PExp> precAssumes) {
        boolean checkList = false;
        if (precAssumes.size() > 0) checkList = true;

        // Loop until we no longer add more expressions or we have added all
        // expressions in the remaining assume expression list.
        while (checkList) {
            boolean formedImplies = false;

            for (PExp assumeExp : precAssumes) {
                // Create a new implies expression if there are common symbols
                // in the assume and in the confirm. (Parsimonious step)
                Set<String> intersection = currentConfirm.getSymbolNames(true, true);
                intersection.retainAll(assumeExp.getSymbolNames(true, true));

                if (!intersection.isEmpty()) {
                    // Don't form implies if we have "Assume true"
                    if (!assumeExp.isObviouslyTrue()) {
                        currentConfirm = g.formImplies(assumeExp, currentConfirm);
                        formedImplies = true;
                    }
                } else {
                    // Form implies if we have "Assume false"
                    if (assumeExp.isLiteralFalse()) {
                        currentConfirm = g.formImplies(assumeExp, currentConfirm);
                        formedImplies = true;
                    }
                }
            }
            checkList = precAssumes.size() > 0 && formedImplies;
        }
        return currentConfirm;
    }

    @NotNull @Override public String getDescription() {
        return "parsimonious assume application";
    }
}