package edu.clemson.resolve.vcgen.application;

import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PSelector;
import edu.clemson.resolve.proving.absyn.PSymbol;
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
        List<PExp> allAssumptions = stat.getAssumeExp().splitIntoConjuncts();
        Map<PExp, PExp> equalitySubstitutions = new HashMap<>();

        List<PExp> remainingAssumptions = new ArrayList<>();

        for (PExp assume : allAssumptions) {
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
                }
                //right replaceability
                else if (rhs.isVariable()) {
                    equalitySubstitutions.put(rhs, lhs);
                }
                //not replaceable...
                else {
                    remainingAssumptions.add(assume);
                }
            }
            else {
                remainingAssumptions.add(assume);
            }
        }
        List<PExp> remainingAssumptionsWithEqualSubt = new ArrayList<>();
        for (PExp assumption : remainingAssumptions) {
            remainingAssumptionsWithEqualSubt.add(assumption.substitute(equalitySubstitutions));
        }
        PExp substitutedConfirm = block.finalConfirm.getConfirmExp()
                .substitute(equalitySubstitutions);
        PExp newFinalConfirm = performParsimoniousStep(block.g, remainingAssumptionsWithEqualSubt,
                substitutedConfirm.splitIntoConjuncts());
        block.finalConfirm(newFinalConfirm);
        return block.snapshot();
    }

    private PExp performParsimoniousStep(DumbMathClssftnHandler g,
                                         List<PExp> assumptions,
                                         List<PExp> confirms) {
        List<PExp> formedImplications = new ArrayList<>();
        for (PExp assume : assumptions) {
            for (PExp confirm : confirms) {
                Set<String> intersection = assume.getSymbolNames(true, true);
                intersection.retainAll(confirm.getSymbolNames(true, true));
                if (!intersection.isEmpty() && !assume.isObviouslyTrue()) {
                    formedImplications.add(g.formImplies(assume, confirm));
                }
            }
        }
        return g.formConjuncts(formedImplications);
    }

    /*
    @NotNull
    @Override
    public AssertiveBlock applyRule(@NotNull Deque<VCAssertiveBlockBuilder> branches,
                                    @NotNull VCAssertiveBlockBuilder block,
                                    @NotNull VCAssume stat) {
        List<PExp> assumeExpList = stat.getAssumeExp().splitIntoConjuncts();
        List<PExp> confirmExpList = block.finalConfirm.getConfirmExp().splitIntoConjuncts();

        PExp newFinalConfirm =
                formParsimoniousVC(block.g, assumeExpList, confirmExpList,
                        stat.isStipulatedAssumption());
        block.finalConfirm(newFinalConfirm);
        return block.snapshot();
    }

    //Using Sami's version; seems to work.
    private PExp formParsimoniousVC(DumbMathClssftnHandler g,
                                    List<PExp> assumeExps,
                                    List<PExp> confirmExps,
                                    boolean isStipulate) {
        List<PExp> remAssumes = new ArrayList<>();

        for (int i = 0; i < confirmExps.size(); i++) {
            PExp currentConfirmExp = confirmExps.get(i);

            List<PExp> assumeExpCopyList = new ArrayList<>();
            for (PExp assumeExp : assumeExps) {
                assumeExpCopyList.add(assumeExp);
            }

            List<PExp> remAssumeExpList = new ArrayList<>();
            for (int j = 0; j < assumeExpCopyList.size(); j++) {
                PExp currentAssumeExp = assumeExpCopyList.get(j);
                PExp tmp;
                boolean hasVerificationVar = false;
                boolean isConceptualVar = false;
                boolean doneReplacement = false;

                // Attempts to simplify equality expressions
                if (currentAssumeExp.isEquality()) {
                    PExp lhs = currentAssumeExp.getSubExpressions().get(1);
                    PExp rhs = currentAssumeExp.getSubExpressions().get(2);

                    boolean isLeftReplaceable = currentAssumeExp.getSubExpressions().get(1) instanceof PSymbol;
                    boolean isRightReplaceable = currentAssumeExp.getSubExpressions().get(2) instanceof PSelector;

                    hasVerificationVar = lhs.isVariable() &&
                            (lhs.getTopLevelOperationName().contains("P_Val") ||
                             lhs.getTopLevelOperationName().contains("conc"));

                    // Check if both the left and right are replaceable
                    if (isLeftReplaceable && isRightReplaceable) {
                        // Only check for verification variable on the left
                        // hand side. If that is the case, we know the
                        // right hand side is the only one that makes sense
                        // in the current context, therefore we do the
                        // substitution.
                        if (hasVerificationVar) {
                            tmp = currentConfirmExp.substitute(lhs, rhs);

                            // Check to see if something has been replaced
                            if (!tmp.equals(currentConfirmExp)) {
                                for (int k = 0; k < remAssumeExpList.size(); k++) {
                                    remAssumeExpList.set(k, remAssumeExpList.get(k).substitute(lhs, rhs));
                                }
                                for (int k = j + 1; k < assumeExpCopyList.size(); k++) {
                                    assumeExpCopyList.set(k, assumeExpCopyList.get(k).substitute(lhs, rhs));
                                }
                                doneReplacement = true;
                            }
                        }
                        else {
                            //Don't do any substitutions, we don't know which makes sense in the current context.
                            tmp = currentConfirmExp;
                        }
                    }
                    // Check if left hand side is replaceable
                    else if (isLeftReplaceable) {
                        // Create a temp expression where left is replaced with the right
                        tmp = currentConfirmExp.substitute(lhs, rhs);

                        //Check to see if something has been replaced
                        doneReplacement = !tmp.equals(currentConfirmExp);

                        //Replace all instances of the left side in the assume expressions we have already processed.
                        for (int k = 0; k < remAssumeExpList.size(); k++) {
                            remAssumeExpList.set(k, remAssumeExpList.get(k).substitute(lhs, rhs));
                        }

                        //Replace all instances of the left side in the assume expressions we haven't processed.
                        for (int k = j + 1; k < assumeExpCopyList.size(); k++) {
                            assumeExpCopyList.set(k, assumeExpCopyList.get(k).substitute(lhs, rhs));
                        }
                    }
                    //Only right hand side is replaceable
                    else if (isRightReplaceable) {
                        // Create a temp expression where right is replaced with the left
                        tmp = currentConfirmExp.substitute(rhs, lhs);

                        //Check to see if something has been replaced
                        doneReplacement = !tmp.equals(currentConfirmExp);

                        //Replace all instances of the left side in the assume expressions we have already processed.
                        for (int k = 0; k < remAssumeExpList.size(); k++) {
                            remAssumeExpList.set(k, remAssumeExpList.get(k).substitute(rhs, lhs));
                        }

                        //Replace all instances of the left side in the assume expressions we haven't processed.
                        for (int k = j + 1; k < assumeExpCopyList.size(); k++) {
                            assumeExpCopyList.set(k, assumeExpCopyList.get(k).substitute(rhs, lhs));
                        }
                    }
                    //Both sides are not replaceable
                    else {
                        tmp = currentConfirmExp;
                    }
                }
                else {
                    tmp = currentConfirmExp;
                }
                // Check to see if this is a stipulate assume clause
                // If yes, we keep a copy of the current
                // assume expression.
                if (isStipulate) {
                    remAssumeExpList.add(currentAssumeExp);
                }
                else {
                    // Update the current confirm expression
                    // if we did a replacement.
                    if (doneReplacement) {
                        currentConfirmExp = tmp;
                    }
                    else {
                        // Check to see if this a verification
                        // variable. If yes, we don't keep this assume.
                        // Otherwise, we need to store this for the
                        // step that generates the parsimonious vcs.
                        if (!hasVerificationVar) {
                            remAssumeExpList.add(currentAssumeExp);
                        }
                    }
                }
            }
            //use the remaining assume expression list;
            //create a new implies expression if there are common symbols
            //in the assume and in the confirm. (Parsimonious step)
            PExp newConfirmExp = formImplies(g, currentConfirmExp, remAssumeExpList, isStipulate);
            confirmExps.set(i, newConfirmExp);
        }
        // Form the return confirm statement
        PExp retExp = g.getTrueExp();
        for (PExp e : confirmExps) {
            if (retExp.isLiteralTrue()) {
                retExp = e;
            }
            else {
                retExp = g.formConjunct(retExp, e);
            }
        }
        return retExp;
    }

    private PExp formImplies(DumbMathClssftnHandler g,
                            PExp confirmExp,
                            List<PExp> remAssumeExpList,
                            boolean isStipulate) {
        if (isStipulate) {
            for (PExp assume : remAssumeExpList) {
                confirmExp = g.formImplies(assume, confirmExp);
            }
            return confirmExp;
        }
        boolean checkList = remAssumeExpList.size() > 0;

        // Loop until we no longer add more expressions or we have added all
        // expressions in the remaining assume expression list.
        while (checkList) {
            List<PExp> tmpExpList = new ArrayList<>();
            boolean formedImplies = false;

            for (PExp assumeExp : remAssumeExpList) {
                // Create a new implies expression if there are common symbols
                // in the assume and in the confirm. (Parsimonious step)
                Set<String> intersection = confirmExp.getSymbolNames(true, true);
                intersection.retainAll(assumeExp.getSymbolNames(true, true));

                if (!intersection.isEmpty()) {
                    // Don't form implies if we have "Assume true"
                    if (!assumeExp.isLiteralTrue()) {
                        confirmExp = g.formImplies(assumeExp, confirmExp);
                        formedImplies = true;
                    }
                }
                else {
                    // Form implies if we have "Assume false"
                    if (assumeExp.isLiteralFalse()) {
                        confirmExp = g.formImplies(assumeExp, confirmExp);
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
        return confirmExp;
    }*/

    @NotNull
    @Override
    public String getDescription() {
        return "Parsimonious assume application";
    }
}