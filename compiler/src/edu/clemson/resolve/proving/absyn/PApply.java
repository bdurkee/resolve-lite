package edu.clemson.resolve.proving.absyn;

import edu.clemson.resolve.misc.Utils;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rsrg.semantics.MTFunction;
import org.rsrg.semantics.MTType;
import org.rsrg.semantics.Quantification;
import org.rsrg.semantics.TypeGraph;

import java.util.*;
import java.util.stream.Collectors;

import static edu.clemson.resolve.misc.Utils.apply;

/**
 * This class represents exclusively function applications, specifically
 * applications with some non-zero number of arguments.
 *
 * @author dtwelch <dtw.welch@gmail.com>
 */
public class PApply extends PExp {

    /**
     * An enumerated type that provides additional information about how to
     * display an instance of {@link PApply}, specifically whether it should be
     * displayed as an infix, outfix, prefix, or postfix style application.
     *
     * <p>
     * Note that while this enum indeed stands-in for the four subclasses we'd
     * otherwise need to represent the application styles mentioned, we still
     * can get specific visitor methods for each style (even with an enum)
     * courtesy of the following accept methods:</p>
     * <ul>
     * <li>{@link #beginAccept(PExpListener, PApply)}</li>
     * <li>{@link #fencepostAccept(PExpListener, PApply)}</li>
     * <li>{@link #endAccept(PExpListener, PApply)}</li>
     * </ul>
     */
    public static enum DisplayStyle {

        /** Traditional prefix style applications of the form: {@code F(x)} */
        PREFIX {
            @Override protected String toString(PApply s) {
                return s.functionPortion.toString() +
                        "(" + Utils.join(s.arguments, ", ") + ")";
            }

            @Override public String getStyleName() {
                return "Prefix";
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
        /** Binary infix style applications where the arguments are on either
         *  side of the function: {@code x F y}
         */
        INFIX {
            @Override protected String toString(PApply s) {
                return Utils.join(s.arguments, " " +
                        s.functionPortion.getCanonicalName() + " ");
            }

            @Override public String getStyleName() {
                return "Infix";
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
        /** Postfix style applications where the operator proceeds its
         *  argumemts: {@code x y F}
         */
        POSTFIX {

            @Override protected String toString(PApply s) {
                String retval = Utils.join(s.arguments, ", ");

                if (s.arguments.size() > 1) {
                    retval = "(" + retval + ")";
                }
                return retval + s.functionPortion.getCanonicalName();
            }

            @Override public String getStyleName() {
                return "Postfix";
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
        /** Outfix style applications where the (single) argument is
         *  sandwitched between the left and rhs operator(s): {@code || F ||}
         */
        OUTFIX {

            @Override protected String toString(PApply s) {
                assert s.functionPortion instanceof PSymbol;
                PSymbol f = (PSymbol)s.functionPortion;
                return f.getLeftPrint() + Utils.join(s.arguments, ", ") +
                        f.getRightPrint();
            }

            @Override public String getStyleName() {
                return "Outfix";
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

        /** Returns a well formatted string representation of this application
         *  style.
         *  @param s some application
         *  @return a string representation.
         */
        protected abstract String toString(PApply s);

        /** Returns a well formatted name for the style */
        public abstract String getStyleName();

        /** Triggers a visit at the start when we first encounter {@code s}. */
        protected abstract void beginAccept(PExpListener v, PApply s);

        /** Triggers a visit in the 'middle'; for internal nodes of {@code s}. */
        protected abstract void fencepostAccept(PExpListener v, PApply s);

        /** Triggers at the 'end' when we're about to leave {@code s}. */
        protected abstract void endAccept(PExpListener v, PApply s);
    }

    /**
     * Represents the 'first class function' this application is referencing.
     * Note that the type of {@code functionPortion} can be considered
     * independent of the types of the actuals
     * (which are rightly embedded here in the argument {@code PExp}s).
     *
     * <p>
     * While this field in most cases will simply be an instance of
     * {@link PSymbol}, realize that it could also be something more 'exotic'
     * such as a {@code PLambda} or even another {@code PApply}.</p>
     */
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

    public MTFunction getConservativePreApplicationType(TypeGraph g) {
        return new MTFunction.MTFunctionBuilder(g, g.EMPTY_SET)
                .paramTypes(arguments.stream()
                        .map(PExp::getMathType)
                        .collect(Collectors.toList())).build();
    }

    public static MTFunction getConservativePreApplicationType(TypeGraph g,
                                                               List<? extends ParseTree> arguments,
                                                               ParseTreeProperty<MTType> types) {
        return new MTFunction.MTFunctionBuilder(g, g.EMPTY_SET)
                .paramTypes(arguments.stream()
                        .map(types::get)
                        .collect(Collectors.toList())).build();
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
            result = new PApplyBuilder(functionPortion.substitute(substitutions))
                    .style(displayStyle)
                    .applicationType(getMathType())
                    .arguments(args).build();
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
                .applicationType(getMathType())
                .applicationTypeValue(getMathTypeValue())
                .style(displayStyle).build();
    }

    @NotNull @Override public PExp withQuantifiersFlipped() {
        return new PApplyBuilder(functionPortion.withQuantifiersFlipped())
                .arguments(apply(arguments, PExp::withQuantifiersFlipped))
                .applicationType(getMathType())
                .applicationTypeValue(getMathTypeValue())
                .style(displayStyle).build();
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

    @Override protected Set<String> getSymbolNamesNoCache(
            boolean excludeApplications, boolean excludeLiterals) {
        Set<String> result = new LinkedHashSet<>();
        if (!excludeApplications) {
            result.addAll(functionPortion
                    .getSymbolNames(false, excludeLiterals));
        }
        for (PExp argument : arguments) {
            result.addAll(argument.getSymbolNames(excludeApplications,
                    excludeLiterals));
        }
        return result;
    }

    @Override public void accept(PExpListener v) {
        v.beginPExp(this);
        v.beginPApply(this);
        displayStyle.beginAccept(v, this);

        v.beginChildren(this);
        functionPortion.accept(v);
        boolean first = true;
        for (PExp arg : arguments) {
            if (!first) {
                displayStyle.fencepostAccept(v, this);
                v.fencepostPApply(this);
            }
            first = false;
            arg.accept(v);
        }
        v.endChildren(this);

        displayStyle.endAccept(v, this);
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

    /** {@inheritDoc} */
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

    /**
     * A mutable, under-construction version of {@code PApply} capable of being
     * incrementally built-up through chained calls to 'builder' methods.
     * <p>
     * When the building is complete, an immutable {@code PApply} instance can
     * be obtained through a call to {@link PApplyBuilder#build()}.</p>
     */
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
