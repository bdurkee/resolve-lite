package edu.clemson.resolve.proving.absyn;

import edu.clemson.resolve.misc.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rsrg.semantics.MTType;
import org.rsrg.semantics.Quantification;

import java.util.*;
import java.util.stream.Collectors;

import static edu.clemson.resolve.misc.Utils.apply;

/**
 * Represents exclusively function applications meaning there is some nonzero
 * number of arguments involved.
 */
public class PApply extends PExp {

    private final PExp functionPortion;
    private final List<PExp> arguments = new ArrayList<>();
    private final Quantification quantification;

    private PApply(PApplyBuilder builder) {
        super(calculateHashes(builder.functionPortion,
                        builder.arguments.iterator()), builder.applicationType,
                builder.applicationTypeValue);
        this.functionPortion = builder.functionPortion;
        this.arguments.addAll(builder.arguments);
        this.quantification = builder.q
    }

    @Override public boolean containsName(String name) {
        boolean result = functionPortion.containsName(name);
        Iterator<PExp> argumentIterator = arguments.iterator();
        while (!result && argumentIterator.hasNext()) {
            result = argumentIterator.next().containsName(name);
        }
        return result;
    }

    @Override @NotNull public PExp substitute(Map<PExp, PExp> substitutions) {
        PExp result;
        if ( substitutions.containsKey(this) ) {
            result = substitutions.get(this);
        }
        else {
            List<PExp> args = arguments.stream()
                    .map(e -> e.substitute(substitutions))
                    .collect(Collectors.toList());
            result = new PApplyBuilder(functionPortion
                    .substitute(substitutions)).arguments(args).build();
        }
        return result;
    }

    @Override @NotNull public List<? extends PExp> getSubExpressions() {
        List<PExp> result = new ArrayList<>();
        result.add(functionPortion);
        result.addAll(arguments);
        return result;
    }

    @Override public boolean isObviouslyTrue() {
        return arguments.size() == 2 &&
                functionPortion.getCanonicalizedName().equals("=") &&
                arguments.get(0).equals(arguments.get(1));
    }

    @Override public boolean isEquality() {
        return arguments.size() == 2 &&
                functionPortion.getCanonicalizedName().equals("=");
    }

    @Override public boolean isLiteralFalse() {
        return false;
    }

    @Override public boolean isVariable() {
        return false;
    }

    @Override protected String getCanonicalizedName() {
        return functionPortion.getCanonicalizedName() +
                "(" + Utils.join(arguments, ", ") + ")";
    }

    @Override public boolean isLiteral() {
        return false;
    }

    @Override public boolean isFunctionApplication() {
        return true;
    }

    @Override protected void splitIntoConjuncts(List<PExp> accumulator) {
        if (arguments.size() == 2 &&
                functionPortion.getCanonicalizedName().equals("and")) {
            arguments.get(0).splitIntoConjuncts(accumulator);
            arguments.get(1).splitIntoConjuncts(accumulator);
        }
        else {
            accumulator.add(this);
        }
    }

    @Override public PExp withIncomingSignsErased() {
        return new PApplyBuilder(functionPortion.withIncomingSignsErased())
                .arguments(apply(arguments, PExp::withIncomingSignsErased))
                .build();
    }

    @Override public PExp withQuantifiersFlipped() {
        return new PApplyBuilder(functionPortion.withQuantifiersFlipped())
                .arguments(apply(arguments, PExp::withQuantifiersFlipped))
                .build();
    }

    //TODO: Ok, here's the thing. I think the type of the set here had better
    //be PExp. I think that if you have something like @A(i), the thing in the
    //set should actually be the application itself: @A(i), not just @A (which
    //is how it would be the way this is currently implemented..). Should also be
    //renamed getIncomingExps or something too.
    @Override public Set<PSymbol> getIncomingVariablesNoCache() {
        Set<PSymbol> result = new LinkedHashSet<>();
        Utils.apply(getSubExpressions(), result, PExp::getIncomingVariables);
        return result;
    }

    @Override public Set<PSymbol> getQuantifiedVariablesNoCache() {
        Set<PSymbol> result = new LinkedHashSet<>();
        Utils.apply(getSubExpressions(), result, PExp::getQuantifiedVariables);
        return result;
    }

    @Override public List<PExp> getFunctionApplicationsNoCache() {
        List<PExp> result = new ArrayList<>();
        result.add(this);
        Utils.apply(getSubExpressions(), result, PExp::getFunctionApplications);
        return result;
    }

    @Override protected Set<String> getSymbolNamesNoCache() {
        Set<String> result = new LinkedHashSet<>();
        Utils.apply(getSubExpressions(), result, PExp::getSymbolNames);
        return result;
    }

    @Override public void accept(PExpListener v) {
        v.beginPExp(this);
        v.beginPApply(this);

        v.beginChildren(this);
        functionPortion.accept(v);
        boolean first = true;
        for (PExp arg : arguments) {
            if (!first) {
                v.fencepostPApply(this);
            }
            first = false;
            arg.accept(v);
        }
        v.endChildren(this);
        v.endPApply(this);
        v.endPExp(this);
    }

    protected static HashDuple calculateHashes(PExp functionPortion,
                                               Iterator<PExp> args) {
        int structureHash = 0;
        int valueHash = 0;
        if (functionPortion != null) {
            valueHash = functionPortion.valueHash;
        }
        if ( args.hasNext() ) {
            structureHash = 17;
            int argMod = 2;
            PExp arg;
            while (args.hasNext()) {
                arg = args.next();
                structureHash += arg.structureHash * argMod;
                valueHash += arg.valueHash * argMod;
                argMod++;
            }
        }
        return new HashDuple(structureHash, valueHash);
    }

    public static class PApplyBuilder implements Utils.Builder<PApply> {

        @NotNull protected final PExp functionPortion;
        @NotNull protected final List<PExp> arguments = new ArrayList<>();

        @NotNull protected Quantification quantification = Quantification.NONE;
        @Nullable protected MTType applicationType, applicationTypeValue;

        public PApplyBuilder(@NotNull PExp functionPortion) {
            this.functionPortion = functionPortion;
        }

        public PApplyBuilder applicationType(@Nullable MTType type) {
            this.applicationType = type;
            return this;
        }

        public PApplyBuilder applicationTypeValue(@Nullable MTType typeValue) {
            this.applicationTypeValue = typeValue;
            return this;
        }

        public PApplyBuilder arguments(@NotNull PExp ... args) {
            arguments(Arrays.asList(args));
            return this;
        }

        public PApplyBuilder arguments(@NotNull Collection<PExp> args) {
            arguments.addAll(args);
            return this;
        }

        public PApplyBuilder quantification(@Nullable Quantification q) {
            this.quantification = q;
            return this;
        }

        @Override @NotNull public PApply build() {
            if (applicationType == null) {
                throw new IllegalStateException("can't build PApply " +
                        "with mathApplicationType==null");
            }
            return new PApply(this);
        }
    }
}
