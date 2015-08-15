package edu.clemson.resolve.proving.absyn;

import edu.clemson.resolve.misc.Utils;
import org.rsrg.semantics.TypeGraph;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.rsrg.semantics.MTFunction;
import org.rsrg.semantics.MTType;
import org.rsrg.semantics.Quantification;
import org.rsrg.semantics.programtype.PTType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A {@link PSymbol} represents a reference to a named element such as a
 * variable, constant, or function. More specifically, all three are represented
 * as function calls, with the former two represented as functions with no
 * arguments.
 */
public class PSymbol extends PExp {

    public static enum DisplayStyle {
        INFIX, OUTFIX, PREFIX
    }

    private final String qualifier, leftPrint, rightPrint, name;

    private final List<PExp> arguments = new ArrayList<>();
    private final boolean literalFlag, incomingFlag;
    private Quantification quantification;
    private final DisplayStyle dispStyle;
    private final List<String> nameComponents = new ArrayList<>();

    private PSymbol(PSymbolBuilder builder) {
        super(calculateHashes(builder.lprint, builder.rprint,
                        builder.arguments.iterator()), builder.mathType,
                builder.mathTypeValue, builder.progType, builder.progTypeValue);
        this.qualifier = builder.qualifier;
        this.name = builder.name;
        this.leftPrint = builder.lprint;
        this.rightPrint = builder.rprint;

        this.arguments.addAll(builder.arguments);
        this.literalFlag = builder.literal;
        this.incomingFlag = builder.incoming;
        this.quantification = builder.quantification;
        this.dispStyle = builder.style;
    }

    protected static HashDuple calculateHashes(List<? extends PExp> args) {
        return calculateHashes("", "", args.iterator());
    }

    protected static HashDuple calculateHashes(String left, String right,
            Iterator<? extends PExp> args) {
        int structureHash;
        int leftHashCode = left.hashCode();
        int valueHash = leftHashCode;

        valueHash *= 59;
        if ( right == null ) {
            valueHash += leftHashCode;
        }
        else {
            valueHash += right.hashCode();
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
        else {
            structureHash = 0;
        }

        return new HashDuple(structureHash, valueHash);
    }

    /**
     * This class represents function applications. The type of a
     * function application is the type of the range of the function. Often we'd
     * like to think about the type of the function itself, not the
     * type of the result of its application. Unfortunately our AST does not
     * consider that the 'function' part of a FunctionExp (as distinct from its
     * parameters) might be a first-class citizen with a type of its own. This
     * method emulates retrieving the (not actually extant) first-class function
     * part and guessing its type. In this case, the guess is "conservative", in
     * that we guess the smallest set that can't be contradicted by the
     * available information. For nodes without a true, first-class function to
     * consult (which, at the moment, is all of them), this means that for the
     * formal parameter types, we'll guess the types of the actual parameters,
     * and for the return type we'll guess Empty_Set (since we
     * have no information about how the return value is used.) This guarantees
     * that the type we return will be a subset of the actual type of the
     * function the RESOLVE programmer intends (assuming she has called it
     * correctly.)
     */
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

    public String getName() {
        return name;
    }

    public String getLeftPrint() {
        return leftPrint;
    }

    public String getRightPrint() {
        return rightPrint;
    }

    public String getQualifier() {
        return qualifier;
    }

    public List<PExp> getArguments() {
        return arguments;
    }

    public Quantification getQuantification() {
        return quantification;
    }

    public boolean isIncoming() {
        return incomingFlag;
    }

    @Override public boolean isEquality() {
        return arguments.size() == 2 && name.equals("=");
    }

    @Override public boolean isFunctionApplication() {
        return arguments.size() > 0;
    }


    @Override public boolean isLiteralFalse() {
        return (arguments.size() == 0 && name.equalsIgnoreCase("false"));
    }

    @Override public boolean isVariable() {
        return !isFunctionApplication();
    }

    @Override public boolean isLiteral() {
        return literalFlag;
    }

    @Override public PExp substitute(Map<PExp, PExp> substitutions) {
        PExp result = substitutions.get(this);

        if ( result == null ) {
            String newName = substituteNamedComponents(substitutions);
            String newLeft = leftPrint, newRight = rightPrint;
            Quantification newQuantification = quantification;

            if ( arguments.size() > 0 && dispStyle.equals(DisplayStyle.PREFIX) ) {
                PSymbol asVariable = new PSymbolBuilder(newName) //
                        .incoming(incomingFlag).literal(literalFlag) //
                        .quantification(quantification) //
                        .mathType(getMathType()) //
                        .mathTypeValue(getMathTypeValue()) //
                        .progType(getProgType()) //
                        .progTypeValue(getProgTypeValue()).build();
                PExp functionSubstitution = substitutions.get(asVariable);

                if ( functionSubstitution != null &&
                        functionSubstitution instanceof PSymbol ) {
                    newLeft = ((PSymbol) functionSubstitution).leftPrint;
                    newRight = ((PSymbol) functionSubstitution).rightPrint;
                    newQuantification =
                            ((PSymbol) functionSubstitution).quantification;
                }
            }

            boolean argumentChanged = false;
            int argIndex = 0;
            Iterator<PExp> argumentsIter = arguments.iterator();

            PExp argument;
            List<PExp> newArgs = new ArrayList<>();
            while (argumentsIter.hasNext()) {
                argument = argumentsIter.next();
                PExp mm = argument.substitute(substitutions);
                newArgs.add(mm);
            }
            PSymbolBuilder temp =
                    (dispStyle == DisplayStyle.OUTFIX) ? new PSymbolBuilder(
                            leftPrint, rightPrint) : new PSymbolBuilder(newName);

            result = temp.mathType(getMathType()) //
                    .mathTypeValue(getMathTypeValue()) //
                    .quantification(newQuantification) //
                    .arguments(newArgs).style(dispStyle) //
                    .incoming(incomingFlag).progType(getProgType()) //
                    .progTypeValue(getProgTypeValue()).build();
        }
        return result;
    }

    /**
     * TODO
     * @return
     */
    private String substituteNamedComponents(Map<PExp, PExp> substitutions) {
        if ( !name.contains(".") ) return name;
        if ( name.contains("...") ) return name;

        List<String> components = Arrays.asList(name.split("\\."));

        for (Map.Entry<PExp, PExp> e : substitutions.entrySet()) {
            for (String c : components) {
                if (!(e.getKey() instanceof PSymbol &&
                        e.getValue() instanceof PSymbol)) {
                    continue;
                }
                if (c.equals(((PSymbol) e.getKey()).getName())) {
                    Collections.replaceAll(components, c,
                            ((PSymbol) e.getValue()).getName());
                }
            }
        }
        return Utils.join(components, ".");
    }

    @Override public boolean isObviouslyTrue() {
        return (arguments.size() == 0 && name.equalsIgnoreCase("true"))
                || (arguments.size() == 2 && name.equals("=") && arguments.get(
                        0).equals(arguments.get(1)));
    }

    @Override public boolean containsName(String name) {
        boolean result = this.name.equals(name);
        Iterator<PExp> argumentIterator = arguments.iterator();
        while (!result && argumentIterator.hasNext()) {
            result = argumentIterator.next().containsName(name);
        }
        return result;
    }


    public List<PExp> experimentalSplit() {
        List<PExp> resultingPartitions = new ArrayList<>();
        TypeGraph g = getMathType().getTypeGraph();
        List<PExp> conjuncts = splitIntoConjuncts();
        PExp curConjuncts = null;
        for (PExp conjunct : conjuncts) {
            if (conjunct.containsName("implies")) {
                List<PExp> a = conjunct.splitOn("implies");
                PExp last = a.get(a.size() - 1);
                List<PExp> sublist = a.subList(0, a.size() - 1);
                PExp conjuncted = g.formConjuncts(sublist);
                PExp result = curConjuncts != null ?
                        g.formConjuncts(curConjuncts, conjuncted) : conjuncted;
                result = g.formImplies(result, last);
                resultingPartitions.add(result);
                curConjuncts = null;
            }
            else {
                if (curConjuncts == null) curConjuncts = conjunct;
                else curConjuncts = g.formConjunct(curConjuncts, conjunct);
            }
        }
        return resultingPartitions;
    }

    @Override public List<? extends PExp> getSubExpressions() {
        return arguments;
    }

    @Override protected void splitIntoConjuncts(List<PExp> accumulator) {
        if ( arguments.size() == 2 && name.equals("and") ) {
            arguments.get(0).splitIntoConjuncts(accumulator);
            arguments.get(1).splitIntoConjuncts(accumulator);
        }
        else {
            accumulator.add(this);
        }
    }

    @Override protected void splitOn(List<PExp> accumulator,
                                     List<String> names) {
        if (names.contains(name)) {
            for (PExp arg : arguments) {
                arg.splitOn(accumulator, names);
            }
        }
        else {
            accumulator.add(this);
        }
    }

    @Override public void accept(PExpVisitor v) {
        v.beginPExp(this);
        v.beginPSymbol(this);
        v.beginChildren(this);

        boolean first = true;
        for (PExp arg : arguments) {
            if ( !first ) {
                v.fencepostPSymbol(this);
            }
            first = false;
            arg.accept(v);
        }
        v.endChildren(this);
        v.endPSymbol(this);
        v.endPExp(this);
    }

    @Override public PExp withIncomingSignsErased() {
        PSymbolBuilder temp =
                (dispStyle == DisplayStyle.OUTFIX) ? new PSymbolBuilder(
                        leftPrint, rightPrint) : new PSymbolBuilder(name);
        PSymbolBuilder result =
                temp.mathType(getMathType()).mathTypeValue(getMathTypeValue())
                        .style(dispStyle).quantification(quantification)
                        .progType(getProgType()).progTypeValue(getProgTypeValue());
        for (PExp arg : arguments) {
            result.arguments(arg.withIncomingSignsErased());
        }
        return result.build();
    }

    @Override public PExp withQuantifiersFlipped() {
        List<PExp> flippedArgs = arguments.stream()
                .map(PExp::withQuantifiersFlipped).collect(Collectors.toList());

        return new PSymbolBuilder(name).literal(literalFlag)
                .incoming(incomingFlag).style(dispStyle).arguments(flippedArgs)
                .mathType(getMathType()).mathTypeValue(getMathTypeValue())
                .progType(getProgType()).progTypeValue(getProgTypeValue())
                .quantification(this.quantification.flipped()).build();
    }

    @Override public Set<PSymbol> getIncomingVariablesNoCache() {
        Set<PSymbol> result = new HashSet<>();
        if ( incomingFlag ) {
            if ( arguments.size() == 0 ) {
                result.add(this);
            }
        }
        for (PExp argument : arguments) {
            result.addAll(argument.getIncomingVariables());
        }
        return result;
    }

    @Override public Set<PSymbol> getQuantifiedVariablesNoCache() {
        Set<PSymbol> result = new HashSet<>();
        if ( quantification != Quantification.NONE ) {
            if ( arguments.size() == 0 ) {
                result.add(this);
            }
            else {
                result.add(new PSymbolBuilder(name).mathType(getMathType())
                        .quantification(quantification).build());
            }
        }
        for (PExp argument : arguments) {
            result.addAll(argument.getQuantifiedVariables());
        }
        return result;
    }

    @Override protected Set<String> getSymbolNamesNoCache() {
        Set<String> result = new HashSet<>();
        if ( quantification == Quantification.NONE ) {
            result.add(getCanonicalName());
        }
        for (PExp argument : arguments) {
            result.addAll(argument.getSymbolNames());
        }
        return result;
    }

    @Override public List<PExp> getFunctionApplicationsNoCache() {
        List<PExp> result = new LinkedList<>();
        if ( this.isFunctionApplication() ) {
            result.add(this);
        }
        for (PExp argument : arguments) {
            result.addAll(argument.getFunctionApplications());
        }
        return result;
    }

    private String getCanonicalName() {
        String result;
        if ( dispStyle.equals(DisplayStyle.OUTFIX) ) {
            result = leftPrint + "..." + rightPrint;
        }
        else {
            result = name;
        }
        return result;
    }

    /**
     * Returns {@code true} <strong>iff</code> this {@code PSymbol} and the
     * provided expression, {@code e}, are equivalent with respect to structure
     * and all function and variable names.
     *
     * @param o The expression to compare this one to.
     * @return {@code true} <strong>iff</strong> {@code this} and the provided
     *         expression are equivalent with respect to structure and all
     *         function and variable names.
     */
    @Override public boolean equals(Object o) {
        boolean result = (o instanceof PSymbol);
        if ( result ) {
            PSymbol oAsPSymbol = (PSymbol) o;

            //Apparently factoring in quantification makes this too strict?
            //but what about literal flags? incoming flags?
            result =
                    (oAsPSymbol.valueHash == valueHash)
                            && name.equals(oAsPSymbol.name)
                            && literalFlag == oAsPSymbol.literalFlag
                            && incomingFlag == oAsPSymbol.incomingFlag
                            && Objects.equals(qualifier, oAsPSymbol.qualifier);

            if ( result ) {
                Iterator<PExp> localArgs = arguments.iterator();
                Iterator<PExp> oArgs = oAsPSymbol.arguments.iterator();

                while (result && localArgs.hasNext() && oArgs.hasNext()) {
                    result = localArgs.next().equals(oArgs.next());
                }
                if ( result ) {
                    result = !(localArgs.hasNext() || oArgs.hasNext());
                }
            }
        }
        return result;
    }

    @Override public String toString() {
        StringBuilder result = new StringBuilder();
        if ( incomingFlag ) result.append("@");
        if ( isFunctionApplication() ) {
            if ( dispStyle == DisplayStyle.INFIX ) {
                result.append(arguments.get(0)).append(" ").append(name)
                        .append(" ").append(arguments.get(1));
            }
            else if ( dispStyle == DisplayStyle.OUTFIX ) {
                result.append(leftPrint).append(arguments.get(0))
                        .append(rightPrint);
            }
            else {
                result.append(name).append("(");
                result.append(Utils.join(arguments, ", ")).append(")");
            }
        }
        else {
            result.append(name);
        }
        if ( this.isFunctionApplication() && this.dispStyle != DisplayStyle.OUTFIX ) {
            return "(" + result.toString() + ")";
        }
        return result.toString();
    }

    public static class PSymbolBuilder implements Utils.Builder<PSymbol> {
        protected final String name, lprint, rprint;
        protected String qualifier;

        protected boolean incoming = false;
        protected boolean literal = false;
        protected DisplayStyle style = DisplayStyle.PREFIX;
        protected Quantification quantification = Quantification.NONE;
        protected MTType mathType, mathTypeValue;
        protected PTType progType, progTypeValue;
        protected final List<PExp> arguments = new ArrayList<>();
        private final List<String> nameComponents = new ArrayList<>();

        public PSymbolBuilder(String name) {
            this(name, null);
        }

        public PSymbolBuilder(String lprint, String rprint) {
            if ( rprint == null ) {
                if ( lprint == null ) {
                    throw new IllegalStateException("null name; all psymbols "
                            + "must be named.");
                }
                rprint = lprint;
                this.name = lprint;
            }
            else {
                this.name = lprint + "..." + rprint;
            }
            this.lprint = lprint;
            this.rprint = rprint;
        }

        public PSymbolBuilder qualifier(Token q) {
            return qualifier(q != null ? q.getText() : null);
        }

        public PSymbolBuilder qualifier(String q) {
            this.qualifier = q;
            return this;
        }

        public PSymbolBuilder literal(boolean e) {
            this.literal = e;
            return this;
        }

        public PSymbolBuilder mathType(MTType e) {
            this.mathType = e;
            return this;
        }

        public PSymbolBuilder mathTypeValue(MTType e) {
            this.mathTypeValue = e;
            return this;
        }

        public PSymbolBuilder progType(PTType e) {
            this.progType = e;
            return this;
        }

        public PSymbolBuilder progTypeValue(PTType e) {
            this.progTypeValue = e;
            return this;
        }

        public PSymbolBuilder quantification(Quantification q) {
            if ( q == null ) {
                q = Quantification.NONE;
            }
            this.quantification = q;
            return this;
        }

        public PSymbolBuilder style(DisplayStyle e) {
            style = e;
            return this;
        }

        public PSymbolBuilder incoming(boolean e) {
            incoming = e;
            return this;
        }

        public PSymbolBuilder argument(PExp e) {
            sanityCheckAddition(e);
            arguments.add(e);
            return this;
        }

        public PSymbolBuilder arguments(PExp... e) {
            arguments(Arrays.asList(e));
            return this;
        }

        public PSymbolBuilder arguments(Collection<PExp> args) {
            sanityCheckAdditions(args);
            arguments.addAll(args);
            return this;
        }

        private void sanityCheckAdditions(Collection<PExp> exps) {
            for (PExp e : exps) {
                sanityCheckAddition(e);
            }
        }

        private void sanityCheckAddition(PExp o) {
            if ( o == null ) {
                throw new IllegalArgumentException("trying to add null element"
                        + " to PSymbol: " + name);
            }
        }

        @Override public PSymbol build() {
            if ( this.mathType == null ) {
                throw new IllegalStateException("mathtype == null; cannot "
                        + "build PExp with null mathtype");
            }
            // System.out.println("building PSymbol name=" + name
            //         + ",quantification=" + quantification);
            return new PSymbol(this);
        }
    }
}
