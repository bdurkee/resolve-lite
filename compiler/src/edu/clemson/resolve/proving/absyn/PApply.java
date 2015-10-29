package edu.clemson.resolve.proving.absyn;

import edu.clemson.resolve.misc.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rsrg.semantics.MTType;

import java.util.*;
import java.util.function.Function;

import static edu.clemson.resolve.misc.Utils.apply;

/**
 * Represents exclusively function applications meaning there is some nonzero
 * number of arguments involved.
 */
public class PApply extends PExp {

    private final PExp functionPortion;
    private final List<PExp> arguments = new ArrayList<>();

    private PApply(PApplyBuilder builder) {
        super(calculateHashes(builder.functionPortion,
                        builder.arguments.iterator()), builder.mathType,
                builder.mathTypeValue);
        this.functionPortion = builder.functionPortion;
        this.arguments.addAll(builder.arguments);
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
     //   collectVariablesByFunction(new LinkedHashSet<PSymbol>(),
      //          PExp::getQuantifiedVariables);
        return null;
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

        @Nullable protected MTType mathType, mathTypeValue;

        public PApplyBuilder(@NotNull PExp functionPortion) {
            this.functionPortion = functionPortion;
        }

        public PApplyBuilder mathType(MTType type) {
            this.mathType = type;
            return this;
        }

        public PApplyBuilder mathTypeValue(@Nullable MTType typeValue) {
            this.mathTypeValue = typeValue;
            return this;
        }

        public PApplyBuilder arguments(PExp ... args) {
            arguments(Arrays.asList(args));
            return this;
        }

        public PApplyBuilder arguments(Collection<PExp> args) {
            arguments.addAll(args);
            return this;
        }

        @Override @NotNull public PApply build() {
            return new PApply(this);
        }
    }
}
