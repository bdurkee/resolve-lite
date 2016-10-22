package edu.clemson.resolve.proving.absyn;

import edu.clemson.resolve.misc.Utils;
import edu.clemson.resolve.semantics.DumbMathClssftnHandler;
import edu.clemson.resolve.semantics.MathClssftn;
import edu.clemson.resolve.semantics.Quantification;
import org.antlr.v4.runtime.Token;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

import static edu.clemson.resolve.misc.Utils.apply;

/**
 * This class represents exclusively (non-nullary) function applications, meaning those with some non-zero number
 * of arguments.
 */
public class PApply extends PExp {

    /**
     * An enumerated type that provides additional information about how to display an instance of {@link PApply},
     * specifically whether it should be displayed as an infix, outfix, prefix, or postfix style application.
     * <p>
     * Note that while this enum indeed stands-in for the four subclasses we'd otherwise need to represent the
     * application styles mentioned, we still can get specific visitor methods for each style (even with an enum)
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
            @Override
            protected String toString(PApply s) {
                if (s.isBracketBasedApp) {
                    return s.arguments.get(0) + "[" + s.arguments.get(1) + "]";
                }
                else {
                    return s.functionPortion.toString() + "(" + Utils.join(s.arguments, ", ") + ")";
                }
            }

            @Override
            protected void beginAccept(PExpListener v, PApply s) {
                v.beginPrefixPApply(s);
            }

            @Override
            protected void fencepostAccept(PExpListener v, PApply s) {
                v.fencepostPrefixPApply(s);
            }

            @Override
            protected void endAccept(PExpListener v, PApply s) {
                v.endPrefixPApply(s);
            }
        },
        /** Binary infix style applications where the arguments are on either side of the function: {@code x F y} */
        INFIX {
            @Override
            protected String toString(PApply s) {
                String result = Utils.join(s.arguments, " " + s.functionPortion.getTopLevelOperationName() + " ");
                return "(" + result + ")";
            }

            @Override
            protected void beginAccept(PExpListener v, PApply s) {
                v.beginInfixPApply(s);
            }

            @Override
            protected void fencepostAccept(PExpListener v, PApply s) {
                v.fencepostInfixPApply(s);
            }

            @Override
            protected void endAccept(PExpListener v, PApply s) {
                v.endInfixPApply(s);
            }
        },
        /** Postfix style applications where the operator proceeds its argumemts: {@code x y F} */
        POSTFIX {
            @Override
            protected String toString(PApply s) {
                String retval = Utils.join(s.arguments, ", ");

                if (s.arguments.size() > 1) {
                    retval = "(" + retval + ")";
                }
                return retval + s.functionPortion.getTopLevelOperationName();
            }

            @Override
            protected void beginAccept(PExpListener v, PApply s) {
                v.beginPostfixPApply(s);
            }

            @Override
            protected void fencepostAccept(PExpListener v, PApply s) {
                v.fencepostPostfixPApply(s);
            }

            @Override
            protected void endAccept(PExpListener v, PApply s) {
                v.endPostfixPApply(s);
            }
        },
        /**
         * Outfix style applications where the (single) argument is sandwitched between the left and
         * rhs operator(s): {@code || F ||}
         */
        OUTFIX {
            @Override
            protected String toString(PApply s) {
                assert s.functionPortion instanceof PSymbol;
                PSymbol f = (PSymbol) s.functionPortion;
                return f.getLeftPrint() + Utils.join(s.arguments, ", ") + f.getRightPrint();
            }

            @Override
            protected void beginAccept(PExpListener v, PApply s) {
                v.beginOutfixPApply(s);
            }

            @Override
            protected void fencepostAccept(PExpListener v, PApply s) {
                v.fencepostOutfixPApply(s);
            }

            @Override
            protected void endAccept(PExpListener v, PApply s) {
                v.endOutfixPApply(s);
            }
        };

        /**
         * Returns a well formatted string representation of this application style.
         *
         * @param s some application
         * @return a string representation.
         */
        protected abstract String toString(PApply s);

        /** Triggers a visit at the start when we first encounter {@code s}. */
        protected abstract void beginAccept(PExpListener v, PApply s);

        /** Triggers a visit in the 'middle'; for internal nodes of {@code s}. */
        protected abstract void fencepostAccept(PExpListener v, PApply s);

        /** Triggers at the 'end' when we're about to leave {@code s}. */
        protected abstract void endAccept(PExpListener v, PApply s);
    }

    /**
     * Represents the 'first class function' this application is referencing. Note that the type of
     * {@code functionPortion} can be considered independent of the types of the formals (which are rightly
     * embedded here in the argument {@code PExp}s).
     * <p>
     * While this exp in most cases will simply be an instance of {@link PSymbol}, realize that it could also be
     * something more 'exotic' such as a {@link PLambda} or even another {@code PApply}.</p>
     */
    private final PExp functionPortion;
    private final List<PExp> arguments = new ArrayList<>();
    private final DisplayStyle displayStyle;

    private final boolean isBracketBasedApp;

    private PApply(@NotNull PApplyBuilder builder) {
        super(calculateHashes(builder.functionPortion, builder.arguments.iterator()),
                builder.applicationType,
                //no; builder.applicationType won't be null; this is checked in PApply:build()
                builder.functionPortion.getProgType(), builder.vcLocation, builder.vcExplanation);
        this.functionPortion = builder.functionPortion;
        this.arguments.addAll(builder.arguments);
        this.displayStyle = builder.displayStyle;
        this.isBracketBasedApp = builder.bracketApp;
    }

    @NotNull
    public PExp getFunctionPortion() {
        return functionPortion;
    }

    @NotNull
    public DisplayStyle getDisplayStyle() {
        return displayStyle;
    }

    @NotNull
    @Override
    public Quantification getQuantification() {
        return functionPortion.getQuantification();
    }

    @NotNull
    public List<PExp> getArguments() {
        return arguments;
    }

    @Override
    public boolean containsName(String name) {
        boolean result = functionPortion.containsName(name);
        Iterator<PExp> argumentIterator = arguments.iterator();
        while (!result && argumentIterator.hasNext()) {
            result = argumentIterator.next().containsName(name);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public PExp substitute(@NotNull Map<PExp, PExp> substitutions) {
        PExp result;
        if (substitutions.containsKey(this)) {
            result = substitutions.get(this);
        }
        else {
            List<PExp> args = arguments.stream()
                    .map(e -> e.substitute(substitutions))
                    .collect(Collectors.toList());
            result = new PApplyBuilder(functionPortion.substitute(substitutions))
                    .style(displayStyle)
                    .applicationType(getMathClssftn())
                    .vcInfo(getVCLocation(), getVCExplanation())
                    .arguments(args).build();
        }
        return result;
    }

    @Override
    @NotNull
    public List<? extends PExp> getSubExpressions() {
        List<PExp> result = new ArrayList<>();
        result.add(functionPortion);
        result.addAll(arguments);
        return result;
    }

    @Override
    public boolean isObviouslyTrue() {
        boolean result = (functionPortion instanceof PSymbol);
        if (result) {
            result = functionPortion.getTopLevelOperationName().equals("=") &&
                    arguments.size() == 2 &&
                    arguments.get(0).equals(arguments.get(1));
        }
        return result;
    }

    @Override
    public boolean isEquality() {
        return arguments.size() == 2 && functionPortion.getTopLevelOperationName().equals("=");
    }

    @Override
    public boolean isConjunct() {
        return arguments.size() == 2 && functionPortion.getTopLevelOperationName().equals("and");
    }

    @NotNull
    @Override
    public String getTopLevelOperationName() {
        return functionPortion.getTopLevelOperationName();
    }

    @Override
    public boolean isFunctionApplication() {
        return true;
    }

    @Override
    public boolean isIncoming() {
        return functionPortion.isIncoming();
    }

    @Override
    protected void splitIntoConjuncts(@NotNull List<PExp> accumulator) {
        if (arguments.size() == 2 &&
                (functionPortion.getTopLevelOperationName().equals("and") ||
                        functionPortion.getTopLevelOperationName().equals("∧"))) {
            arguments.get(0).splitIntoConjuncts(accumulator);
            arguments.get(1).splitIntoConjuncts(accumulator);
        }
        else {
            accumulator.add(this);
        }
    }

    @Override
    public PExp withVCInfo(@Nullable Token location, @Nullable String explanation) {
        PExp name = functionPortion.withVCInfo(location, explanation);
        //distribute vc info down into subexps
        PApplyBuilder builder = new PApplyBuilder(name).vcInfo(location, explanation)
                .applicationType(getMathClssftn())
                .style(displayStyle);

        for (PExp e : getArguments()) {
            builder.arguments(e.withVCInfo(location, explanation));
        }
        return builder.build();
    }

    @NotNull
    public List<PExp> split(PExp assumptions) {
        List<PExp> result = new ArrayList<>();
        DumbMathClssftnHandler g = getMathClssftn().getTypeGraph();
        if (getTopLevelOperationName().equals("and") || getTopLevelOperationName().equals("∧")) {
            arguments.forEach(a -> result.addAll(a.split(assumptions)));
        }
        else if (getTopLevelOperationName().equals("implies") || getTopLevelOperationName().equals("⟹")) {
            PExp tempLeft, tempRight;
            tempLeft = g.formConjuncts(arguments.get(0).splitIntoConjuncts());
            //tempList = arguments.get(0).split(assumptions);
            if (!assumptions.isObviouslyTrue()) {
                tempLeft = g.formConjunct(assumptions, tempLeft);
            }
            tempRight = g.formConjuncts(arguments.get(1).splitIntoConjuncts());
            return arguments.get(1).split(tempLeft);
        }
        else {
            result.add(g.formImplies(assumptions, this));
        }
        return result;
    }

    @NotNull
    @Override
    public PExp withIncomingSignsErased() {
        return new PApplyBuilder(functionPortion.withIncomingSignsErased())
                .arguments(apply(arguments, PExp::withIncomingSignsErased))
                .applicationType(getMathClssftn())
                .vcInfo(getVCLocation(), getVCExplanation())
                .style(displayStyle).build();
    }

    @NotNull
    @Override
    public PExp withQuantifiersFlipped() {
        return new PApplyBuilder(functionPortion.withQuantifiersFlipped())
                .arguments(apply(arguments, PExp::withQuantifiersFlipped))
                .applicationType(getMathClssftn())
                .vcInfo(getVCLocation(), getVCExplanation())
                .style(displayStyle).build();
    }

    @NotNull
    @Override
    public Set<PSymbol> getIncomingVariablesNoCache() {
        Set<PSymbol> result = new LinkedHashSet<>();
        Utils.apply(getSubExpressions(), result, PExp::getIncomingVariables);
        return result;
    }

    @NotNull
    @Override
    public Set<PSymbol> getQuantifiedVariablesNoCache() {
        Set<PSymbol> result = new LinkedHashSet<>();
        Utils.apply(getSubExpressions(), result, PExp::getQuantifiedVariables);
        return result;
    }

    @NotNull
    @Override
    public List<PExp> getFunctionApplicationsNoCache() {
        List<PExp> result = new ArrayList<>();
        result.add(this);
        Utils.apply(getSubExpressions(), result, PExp::getFunctionApplications);
        return result;
    }

    @Override
    protected Set<String> getSymbolNamesNoCache(boolean excludeApplications, boolean excludeLiterals) {
        Set<String> result = new LinkedHashSet<>();
        if (!excludeApplications) {
            result.addAll(functionPortion.getSymbolNames(false, excludeLiterals));
        }
        for (PExp argument : arguments) {
            result.addAll(argument.getSymbolNames(excludeApplications, excludeLiterals));
        }
        return result;
    }


    @NotNull
    @Override
    public Set<PSymbol> getFreeVariablesNoCache() {
        Set<PSymbol> result = new HashSet<>();
        Utils.apply(getSubExpressions(), result, PExp::getFreeVariables);
        return result;
    }

    @Override
    public void accept(PExpListener v) {
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

    protected static HashDuple calculateHashes(@NotNull PExp functionPortion, @NotNull Iterator<PExp> args) {
        int structureHash = 0;
        int valueHash = functionPortion.valueHash;

        if (args.hasNext()) {
            structureHash = 17;
            int argMod = 2;
            PExp arg;
            while (args.hasNext()) {
                arg = args.next();
                if (arg == null) continue;
                structureHash += arg.structureHash * argMod;
                valueHash += arg.valueHash * argMod;
                argMod++;
            }
        }
        return new HashDuple(structureHash, valueHash);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        boolean result = (o instanceof PApply);
        if (result) {
            PApply oAsPApply = (PApply) o;
            result = (oAsPApply.valueHash == valueHash) && functionPortion.equals(oAsPApply.functionPortion);

            if (result) {
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

    @Override
    public String toString() {
        return displayStyle.toString(this);
    }

    /**
     * A mutable, under-construction version of {@code PApply} capable of being incrementally built-up through
     * chained calls to 'builder' methods.
     * <p>
     * When the building is complete, an immutable {@code PApply} instance can be obtained through a call to
     * {@link PApplyBuilder#build()}.</p>
     */
    public static class PApplyBuilder implements Utils.Builder<PApply> {

        protected final PExp functionPortion;
        protected final List<PExp> arguments = new ArrayList<>();
        protected Token vcLocation;
        protected String vcExplanation;

        protected MathClssftn applicationType;
        protected DisplayStyle displayStyle = DisplayStyle.PREFIX;
        protected boolean bracketApp = false;

        /**
         * Constructor for converting an existing function application back into a buildable format. This is useful
         * for adding (or editing) some information in an existing {@link PApply} instance.
         *
         * @param e The existing application.
         */
        public PApplyBuilder(@NotNull PApply e) {
            this.functionPortion = e.functionPortion;
            this.arguments.addAll(e.arguments);
            this.applicationType = e.getMathClssftn();
            this.bracketApp = e.isBracketBasedApp;
            this.displayStyle = e.getDisplayStyle();
            this.vcLocation = e.getVCLocation();
            this.vcExplanation = e.getVCExplanation();
        }

        public PApplyBuilder(@NotNull PExp functionPortion) {
            this.functionPortion = functionPortion;
        }

        public PApplyBuilder style(@NotNull DisplayStyle s) {
            return style(s, false);
        }

        /**
         * The {@code isBracketBasedApp} parameter tells the display style to use square brackets when rendering a
         * prefix function app, rather than the traditional parens (which is default).
         * <p>
         * So, for example, the function {@code f} applied to {@code x} would read {@code f(x)} if
         * {@code isBrackedBasedApp == false}; as opposed to {@code f[x]} otherwise.</p>
         */
        public PApplyBuilder style(@NotNull DisplayStyle s, boolean isBracketBasedApp) {
            this.displayStyle = s;
            this.bracketApp = isBracketBasedApp;
            return this;
        }

        public PApplyBuilder applicationType(@Nullable MathClssftn type) {
            this.applicationType = type;
            return this;
        }

        public PApplyBuilder vcInfo(@Nullable Token vcLocation, @Nullable String vcExplanation) {
            this.vcLocation = vcLocation;
            this.vcExplanation = vcExplanation;
            return this;
        }

        public PApplyBuilder arguments(@NotNull PExp... args) {
            arguments(Arrays.asList(args));
            return this;
        }

        public PApplyBuilder arguments(@NotNull Collection<PExp> args) {
            arguments.addAll(args);
            return this;
        }

        @Override
        @NotNull
        public PApply build() {
            if (applicationType == null) {
                throw new IllegalStateException("can't build PApply with mathAppClssfctn==null");
            }
            return new PApply(this);
        }
    }
}
