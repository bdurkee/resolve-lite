package edu.clemson.resolve.proving.absyn;

import org.antlr.v4.runtime.Token;
import org.jetbrains.annotations.NotNull;
import edu.clemson.resolve.semantics.MathClassification;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

/** A container for a piecewise collection of conditional functions followed by a default, otherwise clause. */
public class PAlternatives extends PExp {

    private final List<Alternative> alternatives;
    private final PExp otherwiseClauseResult;

    public PAlternatives(List<PExp> conditions, List<PExp> results,
                         PExp otherwiseClauseResult, MathClassification type) {
        this(conditions, results, otherwiseClauseResult, type, null, null);
    }

    public PAlternatives(@NotNull List<PExp> conditions,
                         @NotNull List<PExp> results,
                         @Nullable PExp otherwiseClauseResult,
                         @NotNull MathClassification type,
                         @Nullable Token vcLocation,
                         @Nullable String vcExplanation) {
        super(calculateStructureHash(conditions, results,
                otherwiseClauseResult), calculateStructureHash(
                conditions, results, otherwiseClauseResult), type, null, vcLocation, vcExplanation);

        this.alternatives = new ArrayList<>();
        sanityCheckConditions(conditions);

        if (conditions.size() != results.size()) {
            throw new IllegalArgumentException("conditions.size() must equal results.size()");
        }
        Iterator<PExp> conditionIter = conditions.iterator();
        Iterator<PExp> resultIter = results.iterator();
        while (conditionIter.hasNext()) {
            alternatives.add(new Alternative(conditionIter.next(), resultIter.next()));
        }
        this.otherwiseClauseResult = otherwiseClauseResult;
    }

    public void accept(PExpListener v) {
        v.beginPExp(this);
        v.beginPAlternatives(this);
        v.beginChildren(this);

        boolean first = true;
        for (Alternative alt : alternatives) {
            if (!first) {
                v.fencepostPAlternatives(this);
            }
            first = false;
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
            if (!condition.typeMatches(condition.getMathClssftn().getTypeGraph().BOOLEAN)) {
                throw new IllegalArgumentException("AlternativeExps with "
                        + "non-boolean-typed conditions are not accepted "
                        + "by the prover. \n\t" + condition + " has type "
                        + condition.getMathClssftn());
            }
        }
    }

    private static int calculateStructureHash(List<PExp> conditions,
                                              List<PExp> results,
                                              PExp otherwiseClauseResult) {
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

    @NotNull
    @Override
    public List<PExp> getSubExpressions() {
        List<PExp> exps = new LinkedList<>();

        for (Alternative a : alternatives) {
            exps.add(a.result);
            exps.add(a.condition);
        }
        exps.add(otherwiseClauseResult);
        return new ArrayList<>(exps);
    }

    @Override
    public boolean isObviouslyTrue() {
        boolean result = true;

        for (Alternative a : alternatives) {
            result &= a.result.isObviouslyTrue();
        }
        return result && otherwiseClauseResult.isObviouslyTrue();
    }

    @Override
    protected void splitIntoConjuncts(@NotNull List<PExp> accumulator) {
        accumulator.add(this);
    }

    @Override
    public PExp withVCInfo(@Nullable Token location, @Nullable String explanation) {
        List<PExp> conditions = new ArrayList<>();
        List<PExp> results = new ArrayList<>();
        for (Alternative alt : alternatives) {
            conditions.add(alt.condition);
            results.add(alt.result);
        }
        return new PAlternatives(conditions, results, otherwiseClauseResult, getMathClssftn(), location, explanation);
    }

    @NotNull
    @Override
    public PExp withIncomingSignsErased() {
        List<PExp> conditions = new ArrayList<>();
        List<PExp> results = new ArrayList<>();
        for (Alternative alt : alternatives) {
            conditions.add(alt.condition.withIncomingSignsErased());
            results.add(alt.result.withIncomingSignsErased());
        }
        PExp otherwise = otherwiseClauseResult.withIncomingSignsErased();
        return new PAlternatives(conditions, results, otherwise, getMathClssftn());
    }

    @NotNull
    @Override
    public PExp withQuantifiersFlipped() {
        throw new UnsupportedOperationException("This method has not yet been implemented.");
    }

    @NotNull
    @Override
    public Set<PSymbol> getIncomingVariablesNoCache() {
        Set<PSymbol> result = new LinkedHashSet<>();

        for (Alternative a : alternatives) {
            result.addAll(a.condition.getIncomingVariables());
            result.addAll(a.result.getIncomingVariables());
        }
        result.addAll(otherwiseClauseResult.getIncomingVariables());
        return result;
    }

    @NotNull
    @Override
    public PExp substitute(@NotNull Map<PExp, PExp> substitutions) {
        PExp result;

        if (substitutions.containsKey(this)) {
            result = substitutions.get(this);
        }
        else {
            List<PExp> substitutedConditions = new ArrayList<>();
            List<PExp> substitutedResults = new ArrayList<>();
            PExp substitutedOtherwiseResult = otherwiseClauseResult.substitute(substitutions);
            for (Alternative alt : alternatives) {
                substitutedConditions.add(alt.condition.substitute(substitutions));
                substitutedResults.add(alt.result.substitute(substitutions));
            }
            result = new PAlternatives(substitutedConditions,
                    substitutedResults, substitutedOtherwiseResult,
                    getMathClssftn());
        }
        return result;
    }

    @Override
    public boolean containsName(String name) {
        boolean result = false;

        for (Alternative a : alternatives) {
            result |=  a.condition.containsName(name) || a.result.containsName(name);
        }
        return result || otherwiseClauseResult.containsName(name);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{{");
        for (Alternative alternative : alternatives) {
            sb.append(alternative.toString());
        }
        sb.append(otherwiseClauseResult).append(" otherwise;");
        sb.append("}}");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        boolean result = o instanceof PAlternatives;
        if (result) {
            result = otherwiseClauseResult.equals(((PAlternatives) o).otherwiseClauseResult);
            result &= alternatives.size() == ((PAlternatives) o).alternatives.size();
            //now compare the actual alternatives exps
            Iterator<Alternative> thisAltIter = alternatives.iterator();
            Iterator<Alternative> oAltIter = ((PAlternatives) o).alternatives.iterator();
            while (result && thisAltIter.hasNext()) {
                Alternative oAlt = oAltIter.next();
                Alternative thisAlt = thisAltIter.next();
                result = oAlt.condition.equals(thisAlt.condition) && oAlt.result.equals(thisAlt.result);
            }
        }
        return result;
    }

    @Override
    public Set<String> getSymbolNamesNoCache(boolean excludeApplications, boolean excludeLiterals) {
        Set<String> result = new HashSet<>();

        for (Alternative a : alternatives) {
            result.addAll(a.condition.getSymbolNames(excludeApplications, excludeLiterals));
            result.addAll(a.result.getSymbolNames(excludeApplications, excludeLiterals));
        }
        result.addAll(otherwiseClauseResult.getSymbolNames(excludeApplications, excludeLiterals));
        return result;
    }

    @NotNull
    @Override
    public Set<PSymbol> getQuantifiedVariablesNoCache() {
        Set<PSymbol> result = new LinkedHashSet<>(); //i'd like to preserve first found order

        for (Alternative a : alternatives) {
            result.addAll(a.condition.getQuantifiedVariables());
            result.addAll(a.result.getQuantifiedVariables());
        }
        result.addAll(otherwiseClauseResult.getQuantifiedVariables());
        return result;
    }

    @NotNull
    @Override
    public List<PExp> getFunctionApplicationsNoCache() {
        List<PExp> result = new LinkedList<>();

        for (Alternative a : alternatives) {
            result.addAll(a.condition.getFunctionApplications());
            result.addAll(a.result.getFunctionApplications());
        }
        result.addAll(otherwiseClauseResult.getFunctionApplications());
        result.add(this);
        return result;
    }

    @NotNull
    @Override
    protected String getCanonicalName() {
        return "{{ PAlternatives }}";
    }

    private static class UnboxResult implements Function<Alternative, PExp> {
        public final static UnboxResult INSTANCE = new UnboxResult();

        @Override
        public PExp apply(Alternative alternative) {
            return alternative.result;
        }
    }

    private static class UnboxCondition implements Function<Alternative, PExp> {
        public final static UnboxCondition INSTANCE = new UnboxCondition();

        @Override
        public PExp apply(Alternative alternative) {
            return alternative.condition;
        }
    }

    private static class Alternative {
        public final PExp condition, result;

        public Alternative(PExp condition, PExp result) {
            this.condition = condition;
            this.result = result;
        }

        public String toString() {
            return result + " if " + condition + ";";
        }
    }
}