package org.resolvelite.proving.absyn;

import org.resolvelite.semantics.MTType;

import java.util.*;
import java.util.function.Function;

public class PAlternatives extends PExp {

    private final List<Alternative> alternatives;
    private final PExp otherwiseClauseResult;

    public PAlternatives(List<PExp> conditions, List<PExp> results,
            PExp otherwiseClauseResult, MTType type, MTType typeValue) {

        super(
                calculateStructureHash(conditions, results,
                        otherwiseClauseResult), calculateStructureHash(
                        conditions, results, otherwiseClauseResult), type,
                typeValue);

        this.alternatives = new ArrayList<>();
        sanityCheckConditions(conditions);

        if ( conditions.size() != results.size() ) {
            throw new IllegalArgumentException("conditions.size() must equal "
                    + "results.size().");
        }
        Iterator<PExp> conditionIter = conditions.iterator();
        Iterator<PExp> resultIter = results.iterator();
        while (conditionIter.hasNext()) {
            alternatives.add(new Alternative(conditionIter.next(), resultIter
                    .next()));
        }
        this.otherwiseClauseResult = otherwiseClauseResult;
    }

    public void accept(PExpListener v) {
        v.beginPExp(this);
        v.beginPAlternatives(this);
        v.beginChildren(this);

        boolean first = true;
        for (Alternative alt : alternatives) {
            if ( !first ) {
                v.fencepostPAlternatives(this);
            }

            alt.result.accept(v);
            alt.condition.accept(v);
        }
        v.fencepostPAlternatives(this);

        otherwiseClauseResult.accept(v);
        v.endChildren(this);
        v.endPAlternatives(this);
        v.endPExp(this);
    }

    private void sanityCheckConditions(List<PExp> conditions) {
        for (PExp condition : conditions) {
            if ( !condition
                    .typeMatches(condition.getMathType().getTypeGraph().BOOLEAN) ) {
                throw new IllegalArgumentException("AlternativeExps with "
                        + "non-boolean-typed conditions are not accepted "
                        + "by the prover. \n\t" + condition + " has type "
                        + condition.getMathType());
            }
        }
    }

    private static int calculateStructureHash(List<PExp> conditions,
            List<PExp> results, PExp otherwiseClauseResult) {
        int hash = 0;
        Iterator<PExp> conditionIter = conditions.iterator();
        Iterator<PExp> resultIter = conditions.iterator();
        while (conditionIter.hasNext()) {
            hash *= 31;
            hash += conditionIter.next().structureHash;
            hash *= 34;
            hash += resultIter.next().structureHash;
        }
        return hash;
    }

    @Override public List<PExp> getSubExpressions() {
        List<PExp> exps = new LinkedList<>();

        for (Alternative a : alternatives) {
            exps.add(a.result);
            exps.add(a.condition);
        }
        exps.add(otherwiseClauseResult);
        return new ArrayList<>(exps);
    }

    @Override public boolean isObviouslyTrue() {
        boolean result = true;

        for (Alternative a : alternatives) {
            result &= a.result.isObviouslyTrue();
        }
        return result && otherwiseClauseResult.isObviouslyTrue();
    }

    @Override protected void splitIntoConjuncts(List<PExp> accumulator) {
        accumulator.add(this);
    }

    @Override public PExp withIncomingSignsErased() {
        return null;
    }

    @Override public PExp withQuantifiersFlipped() {
        throw new UnsupportedOperationException("This method has not yet "
                + "been implemented.");
    }

    @Override public Set<PSymbol> getIncomingVariablesNoCache() {
        Set<PSymbol> result = new HashSet<>();

        for (Alternative a : alternatives) {
            result.addAll(a.condition.getIncomingVariables());
            result.addAll(a.result.getIncomingVariables());
        }
        result.addAll(otherwiseClauseResult.getIncomingVariables());
        return result;
    }

    @Override public PExp substitute(Map<PExp, PExp> substitutions) {
        PExp retval;

        if ( substitutions.containsKey(this) ) {
            retval = substitutions.get(this);
        }
        else {
            retval = this;
        }
        return retval;
    }

    @Override public boolean containsName(String name) {
        boolean result = false;

        for (Alternative a : alternatives) {
            result |=
                    a.condition.containsName(name)
                            || a.result.containsName(name);
        }
        return result || otherwiseClauseResult.containsName(name);
    }

    @Override public Set<String> getSymbolNamesNoCache() {
        Set<String> result = new HashSet<>();

        for (Alternative a : alternatives) {
            result.addAll(a.condition.getSymbolNames());
            result.addAll(a.result.getSymbolNames());
        }
        result.addAll(otherwiseClauseResult.getSymbolNames());
        return result;
    }

    @Override public Set<PSymbol> getQuantifiedVariablesNoCache() {
        Set<PSymbol> result = new HashSet<>();

        for (Alternative a : alternatives) {
            result.addAll(a.condition.getQuantifiedVariables());
            result.addAll(a.result.getQuantifiedVariables());
        }
        result.addAll(otherwiseClauseResult.getQuantifiedVariables());
        return result;
    }

    @Override public List<PExp> getFunctionApplicationsNoCache() {
        List<PExp> result = new LinkedList<>();

        for (Alternative a : alternatives) {
            result.addAll(a.condition.getFunctionApplications());
            result.addAll(a.result.getFunctionApplications());
        }
        result.addAll(otherwiseClauseResult.getFunctionApplications());
        result.add(this);
        return result;
    }

    @Override public boolean isEquality() {
        return false;
    }

    @Override public boolean isLiteralFalse() {
        return false;
    }

    @Override public boolean isLiteral() {
        return false;
    }

    @Override public boolean isFunction() {
        return false;
    }

    @Override public boolean isVariable() {
        return false;
    }

    private static class UnboxResult implements Function<Alternative, PExp> {
        public final static UnboxResult INSTANCE = new UnboxResult();

        @Override public PExp apply(Alternative alternative) {
            return alternative.result;
        }
    }

    private static class UnboxCondition implements Function<Alternative, PExp> {
        public final static UnboxCondition INSTANCE = new UnboxCondition();

        @Override public PExp apply(Alternative alternative) {
            return alternative.condition;
        }
    }

    private static class Alternative {

        public final PExp condition;
        public final PExp result;

        public Alternative(PExp condition, PExp result) {
            this.condition = condition;
            this.result = result;
        }
    }
}
