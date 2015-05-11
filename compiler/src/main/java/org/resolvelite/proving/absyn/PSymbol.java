package org.resolvelite.proving.absyn;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.resolvelite.misc.Utils;
import org.resolvelite.misc.Utils.Builder;
import org.resolvelite.semantics.MTFunction;
import org.resolvelite.semantics.MTType;
import org.resolvelite.semantics.symbol.Symbol;
import org.resolvelite.typereasoning.TypeGraph;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A {@link PSymbol} represents a reference to a named element such as a
 * variable, constant, or function. More specifically, all three are represented
 * as function calls, with the former two represented as functions with no
 * arguments.
 */
public class PSymbol extends PExp {

    public static enum Quantification {

        NONE {

            protected Quantification flipped() {
                return NONE;
            }
        },
        FOR_ALL {

            protected Quantification flipped() {
                return THERE_EXISTS;
            }
        },
        THERE_EXISTS {

            protected Quantification flipped() {
                return FOR_ALL;
            }
        };

        protected abstract Quantification flipped();
    }

    public static enum DisplayStyle {
        INFIX, OUTFIX, PREFIX
    }

    private final String leftPrint, rightPrint, name;

    private final List<PExp> arguments = new ArrayList<>();
    private final boolean literalFlag, incomingFlag;
    private Quantification quantification;
    private final DisplayStyle dispStyle;

    private PSymbol(PSymbolBuilder builder) {
        super(calculateHashes(builder.lprint, builder.rprint,
                builder.arguments.iterator()), builder.mathType,
                builder.mathTypeValue);

        this.name = builder.name;
        this.leftPrint = builder.lprint;
        this.rightPrint = builder.rprint;
        this.arguments.addAll(builder.arguments);
        this.literalFlag = builder.literal;
        this.incomingFlag = builder.incoming;
        this.quantification = builder.quantification;
        this.dispStyle = builder.style;
    }

    private static HashDuple calculateHashes(String left, String right,
            Iterator<PExp> args) {

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
     * This class represents function <em>applications</em>. The type of a
     * function application is the type of the range of the function. Often we'd
     * like to think about the type of the <em>function itself</em>, not the
     * type of the result of its application. Unfortunately our AST does not
     * consider that the 'function' part of a FunctionExp (as distinct from its
     * parameters) might be a first-class citizen with a type of its own. This
     * method emulates retrieving the (not actually extant) first-class function
     * part and guessing its type. In this case, the guess is "conservative", in
     * that we guess the smallest set that can't be contradicted by the
     * available information. For nodes without a true, first-class function to
     * consult (which, at the moment, is all of them), this means that for the
     * formal parameter types, we'll guess the types of the actual parameters,
     * and for the return type we'll guess <strong>Empty_Set</strong> (since we
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

    public List<PExp> getArguments() {
        return arguments;
    }

    public boolean isIncoming() {
        return incomingFlag;
    }

    @Override public boolean isFunction() {
        return arguments.size() > 0;
    }

    //Todo: This should really check to make sure this.mathType == BOOLEAN.
    //But to do that we need a reference to the typegraph in this hierarchy..
    @Override public boolean isLiteralTrue() {
        return (arguments.size() == 0 && name.equalsIgnoreCase("true"));
    }

    @Override public boolean isLiteralFalse() {
        return (arguments.size() == 0 && name.equalsIgnoreCase("false"));
    }

    @Override public boolean isVariable() {
        return !isFunction();
    }

    @Override public boolean isLiteral() {
        return literalFlag;
    }

    @Override public PExp substitute(Map<PExp, PExp> substitutions) {
        PExp result = substitutions.get(this);

        if ( result == null ) {
            String newLeft = leftPrint, newRight = rightPrint;
            Quantification newQuantification = quantification;

            if ( arguments.size() > 0 && dispStyle.equals(DisplayStyle.PREFIX) ) {
                PSymbol asVariable = new PSymbolBuilder(name) //
                        .incoming(incomingFlag).literal(literalFlag) //
                        .quantification(quantification) //
                        .mathType(getMathType()) //
                        .mathTypeValue(getMathTypeValue()).build();
                PExp functionSubstitution = substitutions.get(asVariable);

                if ( functionSubstitution != null ) {
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

            result = new PSymbolBuilder(name) //
                    .mathType(getMathType()).mathTypeValue(getMathTypeValue()) //
                    .quantification(newQuantification) //
                    .arguments(newArgs).style(dispStyle) //
                    .incoming(incomingFlag).build();
        }
        return result;
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

    @Override public List<PExp> getSubExpressions() {
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

    //Todo.
    @Override public PExp flipQuantifiers() {
        return this;
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
        Iterator<PExp> argumentIter = arguments.iterator();
        Set<PSymbol> argumentVariables;
        while (argumentIter.hasNext()) {
            argumentVariables = argumentIter.next().getQuantifiedVariables();
            result.addAll(argumentVariables);
        }
        return result;
    }

    @Override protected Set<String> getSymbolNamesNoCache() {
        Set<String> result = new HashSet<>();

        if ( quantification == Quantification.NONE ) {
            result.add(getCanonicalName());
        }
        Iterator<PExp> argumentIter = arguments.iterator();
        Set<String> argumentSymbols;
        while (argumentIter.hasNext()) {
            argumentSymbols = argumentIter.next().getSymbolNames();
            result.addAll(argumentSymbols);
        }
        return result;
    }

    @Override public List<PExp> getFunctionApplicationsNoCache() {
        List<PExp> result = new LinkedList<PExp>();
        if ( this.arguments.size() > 0 ) {
            result.add(this);
        }
        Iterator<PExp> argumentIter = arguments.iterator();
        List<PExp> argumentFunctions;
        while (argumentIter.hasNext()) {
            argumentFunctions = argumentIter.next().getFunctionApplications();
            result.addAll(argumentFunctions);
        }
        return result;
    }

    private String getCanonicalName() {
        String result;
        if ( dispStyle.equals(DisplayStyle.OUTFIX) ) {
            result = leftPrint + "_" + rightPrint;
        }
        else {
            result = name;
        }
        return result;
    }

    @Override public boolean equals(Object o) {
        boolean result = (o instanceof PSymbol);
        if ( result ) {
            PSymbol oAsPSymbol = (PSymbol) o;
            result =
                    (oAsPSymbol.valueHash == valueHash)
                            && name.equals(oAsPSymbol.name)
                            && literalFlag == oAsPSymbol.literalFlag
                            && incomingFlag == oAsPSymbol.incomingFlag;

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
        if ( isFunction() ) {
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
        return result.toString();
    }

    public static class PSymbolBuilder implements Builder<PSymbol> {
        protected final String name, lprint, rprint;
        protected boolean incoming = false;
        protected boolean literal = false;
        protected String description;
        protected DisplayStyle style = DisplayStyle.PREFIX;
        protected Quantification quantification = Quantification.NONE;
        protected Token loc;
        protected MTType mathType, mathTypeValue;
        protected final List<PExp> arguments = new ArrayList<>();

        public PSymbolBuilder(String name) {
            this(name, null);
        }

        public PSymbolBuilder(String lprint, String rprint) {
            if ( rprint == null ) {
                if ( lprint == null ) {
                    throw new IllegalStateException("null name; all psymbols "
                            + "symbols must be named.");
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

        public PSymbolBuilder quantification(Quantification q) {
            this.quantification = q;
            return this;
        }

        public PSymbolBuilder style(DisplayStyle e) {
            style = e;
            return this;
        }

        public PSymbolBuilder desc(String desc, Token location) {
            this.description = desc;
            this.loc = location;
            return this;
        }

        public PSymbolBuilder desc(String desc, ParserRuleContext ctx) {
            return desc(desc, ctx != null ? ctx.getStart() : null);
        }

        public PSymbolBuilder incoming(boolean e) {
            incoming = e;
            return this;
        }

        public PSymbolBuilder arguments(PExp... e) {
            arguments(Arrays.asList(e));
            return this;
        }

        public PSymbolBuilder arguments(Collection<PExp> args) {
            arguments.addAll(args);
            return this;
        }

        @Override public PSymbol build() {
            if ( this.mathType == null ) {
                throw new IllegalStateException("mathtype == null; cannot "
                        + "build PExp with null mathtype");
            }
            return new PSymbol(this);
        }
    }

}
