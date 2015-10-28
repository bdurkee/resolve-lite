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
 * Represents a reference to a named element such as a variable, constant, or
 * function.
 *
 * <p>
 * Specifically, when this refers to a name for a funtion, this class therefore
 * represents a typed reference to the first class portion of the function,
 * independent of any supplied arguments.</p>
 *
 * @author dtwelch <dtw.welch@gmail.com>
 */
public class PSymbol extends PExp {

    private final String qualifier, leftPrint, rightPrint, name;

    private final boolean literalFlag, incomingFlag;
    private Quantification quantification;
    private final List<String> nameComponents = new ArrayList<>();

    private PSymbol(PSymbolBuilder builder) {
        super(calculateHashes(builder.lprint, builder.rprint), builder.mathType,
                builder.mathTypeValue, builder.progType, builder.progTypeValue);
        this.qualifier = builder.qualifier;
        this.name = builder.name;
        this.leftPrint = builder.lprint;
        this.rightPrint = builder.rprint;

        this.literalFlag = builder.literal;
        this.incomingFlag = builder.incoming;
        this.quantification = builder.quantification;
    }

    protected static HashDuple calculateHashes(String left, String right) {
        int leftHashCode = left.hashCode();
        int valueHash = leftHashCode;
        valueHash *= 59;
        if ( right == null ) {
            valueHash += leftHashCode;
        }
        else {
            valueHash += right.hashCode();
        }
        return new HashDuple(0, valueHash);
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

    public Quantification getQuantification() {
        return quantification;
    }

    public boolean isIncoming() {
        return incomingFlag;
    }

    @Override public boolean isFunctionApplication() {
        return false;
    }

    @Override public boolean isLiteralFalse() {
        return name.equalsIgnoreCase("false");
    }

    @Override public boolean isVariable() {
        return !isLiteral();
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

            PSymbolBuilder temp =
                    (dispStyle == DisplayStyle.OUTFIX) ? new PSymbolBuilder(
                            leftPrint, rightPrint) : new PSymbolBuilder(newName);

            result = temp.mathType(getMathType())
                    .qualifier(qualifier)
                    .mathTypeValue(getMathTypeValue())
                    .quantification(newQuantification)
                    .incoming(incomingFlag)
                    .literal(literalFlag)
                    .progType(getProgType())
                    .progTypeValue(getProgTypeValue())
                    .build();
        }
        return result;
    }

    /**
     * A helper method to be used alongside this class's {@link #substitute}
     * impl that allows the name of a PSymbol to be segmented into
     * {@code .}-delimited segments. This is useful for instance when we need
     * to replace a {@code PSymbol} such as {@code P.Length} with
     * {@code conc.P.Length}.
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
        return name.equalsIgnoreCase("true");
    }

    @Override public boolean containsName(String name) {
        return this.name.equals(name);
    }

    /*public List<PExp> experimentalSplit(PExp assumptions) {
        List<PExp> result = new ArrayList<>();
        TypeGraph g = getMathType().getTypeGraph();
        if (name.equals("and")) {
            arguments.forEach(a -> result.addAll(a.experimentalSplit(assumptions)));
        }
        else if (name.equals("implies")) {
            PExp tempLeft, tempRight;
            tempLeft = g.formConjuncts(arguments.get(0).splitIntoConjuncts());
            //tempList = arguments.get(0).experimentalSplit(assumptions);
            if (!assumptions.isObviouslyTrue()) {
                tempLeft = g.formConjunct(assumptions, tempLeft);
            }

            tempRight = g.formConjuncts(arguments.get(1).splitIntoConjuncts());
            return arguments.get(1).experimentalSplit(tempLeft);
        }
        else {
            result.add(g.formImplies(assumptions, this));
        }
        return result;
    }*/

    @Override public List<? extends PExp> getSubExpressions() {
        return new ArrayList<>();
    }

    @Override protected void splitIntoConjuncts(List<PExp> accumulator) {
        accumulator.add(this);
    }

    @Override public void accept(PExpListener v) {
        v.beginPExp(this);
        v.beginPSymbol(this);

        //dispStyle.beginAccept(v, this);
        v.beginChildren(this);
        v.endChildren(this);
        //dispStyle.endAccept(v, this);

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

    @Override public PExp withArgumentsErased() {
        return new PSymbolBuilder(this).clearArguments().build();
    }

    @Override public Set<PSymbol> getIncomingVariablesNoCache() {
        Set<PSymbol> result = new LinkedHashSet<>();
        if ( incomingFlag ) {
            result.add(this);
        }
        return result;
    }

    @Override public Set<PSymbol> getQuantifiedVariablesNoCache() {
        Set<PSymbol> result = new HashSet<>();
        if ( quantification != Quantification.NONE ) {
            result.add(this);
        }
        return result;
    }

    @Override protected Set<String> getSymbolNamesNoCache(
            boolean excludeApplications, boolean excludeLiterals) {
        Set<String> result = new HashSet<>();
        if ( !isLiteral() ) {
            result.add(name);
        }
        return result;
    }

    /**
     * This class represents an atomic {@code PExp}. As such, we'll never have
     * any sub-expressions; and hence are guaranteed to contain no
     * applications.
     */
    @Override public List<PExp> getFunctionApplicationsNoCache() {
        return new LinkedList<>();
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

            result =
                    (oAsPSymbol.valueHash == valueHash)
                            && name.equals(oAsPSymbol.name)
                            && literalFlag == oAsPSymbol.literalFlag
                            && incomingFlag == oAsPSymbol.incomingFlag
                            && Objects.equals(qualifier, oAsPSymbol.qualifier);
        }
        return result;
    }

    @Override public String toString() {
        String result = "";
        if ( incomingFlag ) result += "@";
        return result += name;
    }

    public static class PSymbolBuilder implements Utils.Builder<PSymbol> {
        protected String name, lprint, rprint;
        protected String qualifier;

        protected boolean incoming = false;
        protected boolean literal = false;
        protected Quantification quantification = Quantification.NONE;
        protected MTType mathType, mathTypeValue;
        protected PTType progType, progTypeValue;
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

        public PSymbolBuilder name(String name) {
            this.name = name;
            return this;
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

        public PSymbolBuilder incoming(boolean e) {
            incoming = e;
            return this;
        }

        @Override public PSymbol build() {
            if ( this.mathType == null ) {
                throw new IllegalStateException("mathtype == null; cannot "
                        + "build PExp with null mathtype");
            }
            //System.out.println("building PSymbol name="+name+",quantification="+quantification);
            return new PSymbol(this);
        }
    }
}
