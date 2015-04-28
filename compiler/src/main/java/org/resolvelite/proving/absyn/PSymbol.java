package org.resolvelite.proving.absyn;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
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

    public static enum DisplayStyle {
        INFIX, OUTFIX, PREFIX
    }

    private final String leftPrint, rightPrint, name;

    private final List<PExp> arguments = new ArrayList<>();
    private final boolean literalFlag, incomingFlag;
    private Symbol.Quantification quantification;
    private final DisplayStyle dispStyle;

    private PSymbol(PSymbolBuilder builder) {
        super(builder.mathType, builder.mathTypeValue);
        this.name = builder.name;
        this.leftPrint = builder.lprint;
        this.rightPrint = builder.rprint;
        this.arguments.addAll(builder.arguments);
        this.literalFlag = builder.literal;
        this.incomingFlag = builder.incoming;
        this.quantification = builder.quantification;
        this.dispStyle = builder.style;
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

    public List<PExp> getArguments() {
        return arguments;
    }

    public boolean isIncoming() {
        return incomingFlag;
    }

    @Override public boolean isFunction() {
        return arguments.size() > 0;
    }

    @Override public boolean isObviouslyTrue() {
        return (arguments.size() == 0 && name.equalsIgnoreCase("true"))
                || (arguments.size() == 2 && name.equals("=") && arguments.get(
                        0).equals(arguments.get(1)));
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
            Symbol.Quantification newQuantification = quantification;

            if ( arguments.size() > 0 && dispStyle.equals(DisplayStyle.PREFIX) ) {
                PSymbol asVariable = new PSymbolBuilder(leftPrint) //
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

            result = new PSymbolBuilder(newLeft, newRight) //
                    .mathType(getMathType()).mathType(getMathTypeValue()) //
                    .quantification(newQuantification) //
                    .style(dispStyle) //
                    .incoming(incomingFlag).build();
        }
        return result;
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

    @Override public boolean equals(Object o) {
        boolean result = (o instanceof PSymbol);

        if ( result ) {
            PSymbol oAsPSymbol = (PSymbol) o;
            result = name.equals(oAsPSymbol.name);

            if ( result ) {
                Iterator<PExp> thisArgs = arguments.iterator();
                Iterator<PExp> oArgs = oAsPSymbol.arguments.iterator();

                while (result && thisArgs.hasNext() && oArgs.hasNext()) {
                    result = thisArgs.next().equals(oArgs.next());
                }
                if ( result ) {
                    result = !(thisArgs.hasNext() || oArgs.hasNext());
                }
            }
        }
        return result;
    }

    public static class PSymbolBuilder implements Builder<PSymbol> {
        protected final String name, lprint, rprint;
        protected boolean incoming = false;
        protected boolean literal = false;

        protected DisplayStyle style = DisplayStyle.PREFIX;
        protected Symbol.Quantification quantification =
                Symbol.Quantification.NONE;

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

        public PSymbolBuilder quantification(Symbol.Quantification q) {
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
                        + "build mexp with null mathtype");
            }
            return new PSymbol(this);
        }
    }
}
