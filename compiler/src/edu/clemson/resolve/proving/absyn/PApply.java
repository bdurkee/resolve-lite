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
 *
 * @since 0.0.1
 */
public class PApply extends PExp {

    public static enum DisplayStyle {

        PREFIX {
            @Override protected String toString(PApply s) {
                return s.functionPortion.toString() +
                        "(" + Utils.join(s.arguments, ", ") + ")";
            }

            @Override protected void beginAccept(PExpListener v, PApply s) {
                v.beginPrefixPApply(s);
            }

            @Override protected void fencepostAccept(PExpListener v, PApply s) {
                v.fencepostPrefixPApply(s);
            }

            @Override protected void endAccept(PExpListener v, PApply s) {
                v.endPrefixPApply(s);
            }
        },
        INFIX {
            @Override protected String toString(PApply s) {
                return Utils.join(s.arguments, " " +
                        s.functionPortion.getCanonicalName() + " ");
            }

            @Override protected void beginAccept(PExpListener v, PApply s) {
                v.beginInfixPApply(s);
            }

            @Override protected void fencepostAccept(PExpListener v, PApply s) {
                v.fencepostInfixPApply(s);
            }

            @Override protected void endAccept(PExpListener v, PApply s) {
                v.endInfixPApply(s);
            }
        },
        POSTFIX {

            @Override protected String toString(PApply s) {
                String retval = Utils.join(s.arguments, ", ");

                if (s.arguments.size() > 1) {
                    retval = "(" + retval + ")";
                }
                return retval + s.functionPortion.getCanonicalName();
            }

            @Override protected void beginAccept(PExpListener v, PApply s) {
                v.beginPostfixPApply(s);
            }

            @Override protected void fencepostAccept(PExpListener v, PApply s) {
                v.fencepostPostfixPApply(s);
            }

            @Override protected void endAccept(PExpListener v, PApply s) {
                v.endPostfixPApply(s);
            }
        },
        OUTFIX {

            @Override protected String toString(PApply s) {
                assert s.functionPortion instanceof PSymbol;
                PSymbol f = (PSymbol)s.functionPortion;
                return f.getLeftPrint() + Utils.join(s.arguments, ", ") +
                        f.getRightPrint();
            }

            @Override protected void beginAccept(PExpListener v, PApply s) {
                v.beginOutfixPApply(s);
            }

            @Override protected void fencepostAccept(PExpListener v, PApply s) {
                v.fencepostOutfixPApply(s);
            }

            @Override protected void endAccept(PExpListener v, PApply s) {
                v.endOutfixPApply(s);
            }
        };

        protected abstract String toString(PApply s);

        protected abstract void beginAccept(PExpListener v, PApply s);

        protected abstract void fencepostAccept(PExpListener v, PApply s);

        protected abstract void endAccept(PExpListener v, PApply s);
    }

    @NotNull private final PExp functionPortion;
    @NotNull private final List<PExp> arguments = new ArrayList<>();
    @NotNull private final DisplayStyle displayStyle;

    private PApply(@NotNull PApplyBuilder builder) {
        super(calculateHashes(builder.functionPortion,
                        builder.arguments.iterator()), builder.applicationType,
        //no; builder.applicationType won't be null; this is checked in PApply:build()
                builder.applicationTypeValue);
        this.functionPortion = builder.functionPortion;
        this.arguments.addAll(builder.arguments);
        this.displayStyle = builder.displayStyle;
    }

    @NotNull public PExp getFunctionPortion() {
        return functionPortion;
    }

    @NotNull public DisplayStyle getDisplayStyle() {
        return displayStyle;
    }

    @NotNull @Override public Quantification getQuantification() {
        return functionPortion.getQuantification();
    }

    @NotNull public List<PExp> getArguments() {
        return arguments;
    }

    @Override public boolean containsName(String name) {
        boolean result = functionPortion.containsName(name);
        Iterator<PExp> argumentIterator = arguments.iterator();
        while (!result && argumentIterator.hasNext()) {
            result = argumentIterator.next().containsName(name);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     *
     */
    @NotNull @Override public PExp substitute(
            @NotNull Map<PExp, PExp> substitutions) {
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
        boolean result = (functionPortion instanceof PSymbol);
        if (result) {
            result = functionPortion.getCanonicalName().equals("=") &&
                arguments.size() == 2 &&
                    arguments.get(0).equals(arguments.get(1));
        }
        return result;
    }

    @Override public boolean isEquality() {
        return arguments.size() == 2 &&
                functionPortion.getCanonicalName().equals("=");
    }

    @NotNull @Override protected String getCanonicalName() {
        return functionPortion.getCanonicalName();
    }

    @Override public boolean isFunctionApplication() {
        return true;
    }

    @Override public boolean isIncoming() {
        return functionPortion.isIncoming();
    }

    @Override protected void splitIntoConjuncts(
            @NotNull List<PExp> accumulator) {
        System.out.println("Canonical name is: " + functionPortion.getCanonicalName());
        System.out.println("arg ct is: " + arguments.size());

        if (arguments.size() == 2 &&
                functionPortion.getCanonicalName().equals("and")) {
            arguments.get(0).splitIntoConjuncts(accumulator);
            arguments.get(1).splitIntoConjuncts(accumulator);
        }
        else {
            accumulator.add(this);
        }
    }

    @NotNull @Override public PExp withIncomingSignsErased() {
        return new PApplyBuilder(functionPortion.withIncomingSignsErased())
                .arguments(apply(arguments, PExp::withIncomingSignsErased))
                .build();
    }

    @NotNull @Override public PExp withQuantifiersFlipped() {
        return new PApplyBuilder(functionPortion.withQuantifiersFlipped())
                .arguments(apply(arguments, PExp::withQuantifiersFlipped))
                .build();
    }

    @NotNull @Override public Set<PSymbol> getIncomingVariablesNoCache() {
        Set<PSymbol> result = new LinkedHashSet<>();
        Utils.apply(getSubExpressions(), result, PExp::getIncomingVariables);
        return result;
    }

    @NotNull @Override public Set<PSymbol> getQuantifiedVariablesNoCache() {
        Set<PSymbol> result = new LinkedHashSet<>();
        Utils.apply(getSubExpressions(), result, PExp::getQuantifiedVariables);
        return result;
    }

    @NotNull @Override public List<PExp> getFunctionApplicationsNoCache() {
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

    protected static HashDuple calculateHashes(@NotNull PExp functionPortion,
                                               @NotNull Iterator<PExp> args) {
        int structureHash = 0;
        int valueHash = functionPortion.valueHash;

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

    @Override public boolean equals(Object o) {
        boolean result = (o instanceof PApply);
        if (result) {
            PApply oAsPApply = (PApply)o;
            result = (oAsPApply.valueHash == valueHash)
                    && functionPortion.equals(oAsPApply.functionPortion);

            if ( result ) {
                Iterator<PExp> localArgs = arguments.iterator();
                Iterator<PExp> oArgs = oAsPApply.arguments.iterator();

                while (result && localArgs.hasNext() && oArgs.hasNext()) {
                    result = localArgs.next().equals(oArgs.next());
                }
                if (result) {
                    result = !(localArgs.hasNext() || oArgs.hasNext());
                }
            }
        }
        return result;
    }

    @Override public String toString() {
        return displayStyle.toString(this);
    }

    public static class PApplyBuilder implements Utils.Builder<PApply> {

        @NotNull protected final PExp functionPortion;
        @NotNull protected final List<PExp> arguments = new ArrayList<>();

        @Nullable protected MTType applicationType, applicationTypeValue;
        @NotNull protected DisplayStyle displayStyle = DisplayStyle.PREFIX;

        public PApplyBuilder(@NotNull PExp functionPortion) {
            this.functionPortion = functionPortion;
        }

        public PApplyBuilder style(@NotNull DisplayStyle s) {
            this.displayStyle = s;
            return this;
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

        @Override @NotNull public PApply build() {
            if (applicationType == null) {
                throw new IllegalStateException("can't build PApply " +
                        "with mathApplicationType==null");
            }
            return new PApply(this);
        }
    }
}
