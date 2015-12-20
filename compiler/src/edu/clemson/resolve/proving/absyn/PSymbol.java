package edu.clemson.resolve.proving.absyn;

import edu.clemson.resolve.misc.Utils;
import org.jetbrains.annotations.NotNull;
import org.antlr.v4.runtime.Token;
import org.jetbrains.annotations.Nullable;
import org.rsrg.semantics.MTType;
import org.rsrg.semantics.Quantification;
import org.rsrg.semantics.TypeGraph;
import org.rsrg.semantics.programtype.PTType;

import java.util.*;

/** Represents a reference to a named element such as a variable, constant, or
 *  function.
 *  <p>
 *  Specifically, if this refers to a <em>name</em> of a funtion, then that
 *  instance represents a typed reference to the first class portion of the function,
 *  independent of any supplied arguments.</p>
 */
public class PSymbol extends PExp {

    @NotNull private final Quantification quantification;
    @NotNull private final List<String> nameComponents = new ArrayList<>();

    @NotNull private final String name;
    @Nullable private final String qualifier, leftPrint, rightPrint;

    private final boolean literalFlag, incomingFlag;

    /** Constructs a new {@code PSymbol}. Note that this is specifically made
     *  private; thus clients should instead go through {@link PSymbolBuilder}
     *  to construct new instances.
     *
     *  @param builder a 'buildable' version of {@code PSymbol}
     */
    private PSymbol(PSymbolBuilder builder) {
        super(calculateHashes(builder.name), builder.mathType,
                builder.mathTypeValue, builder.progType, builder.progTypeValue);
        this.qualifier = builder.qualifier;
        this.name = builder.name;
        this.leftPrint = builder.lprint;
        this.rightPrint = builder.rprint;

        this.literalFlag = builder.literal;
        this.incomingFlag = builder.incoming;
        this.quantification = builder.quantification;
    }

    protected static HashDuple calculateHashes(String name) {
        int valueHash = name.hashCode();
        valueHash *= 59;
        return new HashDuple(0, valueHash);
    }

    @NotNull public String getName() {
        return name;
    }

    @Nullable public String getLeftPrint() {
        return leftPrint;
    }

    @Nullable public String getRightPrint() {
        return rightPrint;
    }

    @Nullable public String getQualifier() {
        return qualifier;
    }

    @NotNull public Quantification getQuantification() {
        return quantification;
    }

    @Override public boolean isIncoming() {
        return incomingFlag;
    }

    @NotNull @Override public String getCanonicalName() {
        return getName();
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

    @NotNull @Override public PExp substitute(
            @NotNull Map<PExp, PExp> substitutions) {
        PExp result = substitutions.get(this);
        if (result == null) {
            String newName = substituteNamedComponents(substitutions);
            String newLeft = leftPrint, newRight = rightPrint;
            result = new PSymbolBuilder(this).build();
        }
        return result;
    }

    /** A helper method to be used alongside this class's
     *  {@link PExp PExp#substitute} that allows the name of a PSymbol to be
     *  segmented into {@code .}-delimited segments. This is useful for instance
     *  when we need to replace a {@code PSymbol} such as {@code P.Length} with
     *  {@code conc.P.Length}.
     */
    private String substituteNamedComponents(Map<PExp, PExp> substitutions) {
        if (!name.contains(".")) return name;
        if (name.contains("...")) return name;

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

    @NotNull public List<PExp> splitIntoSequents(PExp assumptions) {
        List<PExp> result = new ArrayList<>();
        TypeGraph g = getMathType().getTypeGraph();
        result.add(g.formImplies(assumptions, this));
        return result;
    }

    @NotNull @Override public List<? extends PExp> getSubExpressions() {
        return new ArrayList<>();
    }

    @Override protected void splitIntoConjuncts(@NotNull List<PExp> accumulator) {
        accumulator.add(this);
    }

    @Override public void accept(PExpListener v) {
        v.beginPExp(this);
        v.beginPSymbol(this);

        v.beginChildren(this);
        v.endChildren(this);

        v.endPSymbol(this);
        v.endPExp(this);
    }

    @NotNull @Override public PExp withIncomingSignsErased() {
        return new PSymbolBuilder(this).incoming(false).build();
    }

    @NotNull @Override public PExp withQuantifiersFlipped() {
        return new PSymbolBuilder(this)
                .quantification(quantification.flipped())
                .build();
    }

    @NotNull @Override public Set<PSymbol> getIncomingVariablesNoCache() {
        Set<PSymbol> result = new LinkedHashSet<>();
        if (incomingFlag) {
            result.add(this);
        }
        return result;
    }

    @NotNull @Override public Set<PSymbol> getQuantifiedVariablesNoCache() {
        Set<PSymbol> result = new LinkedHashSet<>();
        if (quantification != Quantification.NONE) {
            result.add(this);
        }
        return result;
    }

    @NotNull @Override protected Set<String> getSymbolNamesNoCache(
            boolean excludeApplications,
            boolean excludeLiterals) {
        Set<String> result = new HashSet<>();
        if (!(excludeApplications && isFunctionApplication()) &&
                !(excludeLiterals && isLiteral()) &&
                quantification == Quantification.NONE ) {
            result.add(getCanonicalName());
        }
        return result;
    }

    /**
     * This class represents an atomic {@code PExp}. As such, we'll never have
     * any sub-expressions; and hence are guaranteed to contain no
     * applications.
     */
    @NotNull @Override public List<PExp> getFunctionApplicationsNoCache() {
        return new LinkedList<>();
    }

    @Override public boolean equals(Object o) {
        boolean result = (o instanceof PSymbol);
        if (result) {
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
        if (incomingFlag) result += "@";
        if (leftPrint != null && rightPrint != null) {
            result = leftPrint + result + rightPrint;
        }
        return result += name;
    }

    /** A builder for {@code PSymbol}s.
     *  <p>
     *  As usual, a final, immutable instance of {@link PSymbol} can be obtained
     *  through a call to {@link PSymbolBuilder#build()}.</p>
     */
    public static class PSymbolBuilder implements Utils.Builder<PSymbol> {
        protected String name, lprint, rprint;
        protected String qualifier;

        protected boolean incoming = false;
        protected boolean literal = false;
        protected Quantification quantification = Quantification.NONE;
        protected MTType mathType, mathTypeValue;
        protected PTType progType, progTypeValue;

        public PSymbolBuilder(PSymbol existingPSymbol) {
            this.name = existingPSymbol.getName();
            this.qualifier = existingPSymbol.getQualifier();
            this.lprint = existingPSymbol.getLeftPrint();
            this.rprint = existingPSymbol.getRightPrint();
            this.literal = existingPSymbol.isLiteral();
            this.incoming = existingPSymbol.isIncoming();
            this.quantification = existingPSymbol.getQuantification();

            this.mathType = existingPSymbol.getMathType();
            this.mathTypeValue = existingPSymbol.getMathTypeValue();
            this.progType = existingPSymbol.getProgType();
            this.progTypeValue = existingPSymbol.getProgTypeValue();
        }

        public PSymbolBuilder(PSymbol existingPSymbol, String newName) {
            this.name = newName;
            this.qualifier = existingPSymbol.getQualifier();
            this.lprint = existingPSymbol.getLeftPrint();
            this.rprint = existingPSymbol.getRightPrint();
            this.literal = existingPSymbol.isLiteral();
            this.incoming = existingPSymbol.isIncoming();
            this.quantification = existingPSymbol.getQuantification();

            this.mathType = existingPSymbol.getMathType();
            this.mathTypeValue = existingPSymbol.getMathTypeValue();
            this.progType = existingPSymbol.getProgType();
            this.progTypeValue = existingPSymbol.getProgTypeValue();
        }

        public PSymbolBuilder(String name) {
            this.name = name;
        }

        public PSymbolBuilder(String lprint, String rprint) {
            if (rprint == null ) {
                if (lprint == null) {
                    throw new IllegalStateException("null name; all psymbols "
                            + "must be named.");
                }
                rprint = lprint;
            }
            this.name = lprint + "..." + rprint;
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
            if (q == null) {
                q = Quantification.NONE;
            }
            this.quantification = q;
            return this;
        }

        public PSymbolBuilder incoming(boolean e) {
            incoming = e;
            return this;
        }

        @Override @NotNull public PSymbol build() {
            if (this.mathType == null) {
                throw new IllegalStateException("mathtype == null; cannot "
                        + "build PExp with null mathtype");
            }
            //System.out.println("building PSymbol name="+name+",quantification="+quantification);
            return new PSymbol(this);
        }
    }
}
