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

    @NotNull @Override public AssertiveBlock applyRule(
            @NotNull VCAssertiveBlockBuilder block,
            @NotNull VCAssume stat) {
        PExp assumeExp = stat.getAssumeExp();
        PExp RP = block.finalConfirm.getConfirmExp();

        Map<PExp, PExp> concEqualitySubstitutions = new LinkedHashMap<>();
        List<PExp> assumeConjunctsWithoutConcEqualities = new LinkedList<>();
        for (PExp assume : assumeExp.splitIntoConjuncts()) {
            boolean isConceptual = false;
            if (assume.isEquality()) {
                PExp lhs = assume.getSubExpressions().get(1);
                PExp rhs = assume.getSubExpressions().get(2);
                if (lhs.isVariable() && lhs.containsName("conc")) {
                    concEqualitySubstitutions.put(lhs, rhs);
                    isConceptual = true;
                }
            }
            if (!isConceptual) {
                assumeConjunctsWithoutConcEqualities.add(assume);
            }
        }
        //now substitute any conc equalities into RP
        RP = RP.substitute(concEqualitySubstitutions);
        //beta reduce any lambdas present now...

        List<PExp> parsimoniousAssumeConjuncts = new LinkedList<>();
        for (PExp assume : assumeConjunctsWithoutConcEqualities) {
            Set<String> intersection = assumeExp.getSymbolNames(true, true);
            intersection.retainAll(RP.getSymbolNames(true, true));
            if (!intersection.isEmpty() && !assume.isObviouslyTrue()) {
                parsimoniousAssumeConjuncts.add(assume);
            }
        }
        //this will be the pruned assume expr
        if (!parsimoniousAssumeConjuncts.isEmpty()) {
            assumeExp = block.g.formConjuncts(parsimoniousAssumeConjuncts);
            block.finalConfirm(block.g.formImplies(assumeExp, RP));
        }
        else {
            block.finalConfirm(RP);
        }
        return block.snapshot();
    }

    @NotNull @Override public String getDescription() {
        return "parsimonious assume application";
    }
}