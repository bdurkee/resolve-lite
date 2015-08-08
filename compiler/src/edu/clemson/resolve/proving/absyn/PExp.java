package edu.clemson.resolve.proving.absyn;

import edu.clemson.resolve.vcgen.VCPartitioningListener;
import org.rsrg.semantics.MTType;
import org.rsrg.semantics.TypeGraph;
import org.rsrg.semantics.programtype.PTType;

import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;

public abstract class PExp {

    public final int structureHash;
    public final int valueHash;
    private final MTType type, typeValue;

    /**
     * Since the removal of the Exp hierarchy, the role of PExps has expanded
     * considerably.
     * <p>
     * In other words, if this {@code PExp} was born out of a
     * programmatic expression (for vcgen), program type info should be
     * present, if not, then these should/will be {@code null}.</p>
     */
    private final PTType progType, progTypeValue;

    private Set<String> cachedSymbolNames = null;
    private List<PExp> cachedFunctionApplications = null;
    private Set<PSymbol> cachedQuantifiedVariables = null;
    private Set<PSymbol> cachedIncomingVariables = null;

    public PExp(PSymbol.HashDuple hashes, MTType type, MTType typeValue) {
        this(hashes.structureHash, hashes.valueHash, type, typeValue, null,
                null);
    }

    public PExp(PSymbol.HashDuple hashes, MTType type, MTType typeValue,
                PTType progType, PTType progTypeValue) {
        this(hashes.structureHash, hashes.valueHash, type, typeValue, progType,
                progTypeValue);
    }

    public PExp(int structureHash, int valueHash, MTType type, MTType typeValue) {
        this(structureHash, valueHash, type, typeValue, null, null);
    }

    public PExp(int structureHash, int valueHash, MTType type,
                MTType typeValue, PTType progType, PTType progTypeValue) {
        this.type = type;
        this.typeValue = typeValue;
        this.progType = progType;
        this.progTypeValue = progTypeValue;
        this.structureHash = structureHash;
        this.valueHash = valueHash;
    }

    @Override public int hashCode() {
        return valueHash;
    }

    public final PTType getProgType() {
        return progType;
    }

    public final PTType getProgTypeValue() {
        return progTypeValue;
    }

    public final MTType getMathType() {
        return type;
    }

    public final MTType getMathTypeValue() {
        return typeValue;
    }

    public PExp substitute(List<? extends PExp> currents, PExp repl) {
        Map<PExp, PExp> substitutions = new HashMap<>();
        for (PExp current : currents) {
            substitutions.put(current, repl);
        }
        return substitute(substitutions);
    }

    public PExp substitute(List<? extends PExp> currents, PExp... repls) {
        return substitute(currents, Arrays.asList(repls));
    }

    public PExp substitute(List<? extends PExp> currents,
            List<? extends PExp> replacements) {
        if ( currents.size() != replacements.size() ) {
            throw new IllegalArgumentException("substitution lists must be"
                    + "the same length");
        }
        Iterator<? extends PExp> replIter = replacements.iterator();
        Iterator<? extends PExp> currIter = currents.iterator();
        Map<PExp, PExp> result = new LinkedHashMap<>();
        while (replIter.hasNext()) {
            result.put(currIter.next(), replIter.next());
        }
        return substitute(result);
    }

    public PExp substitute(PExp current, PExp replacement) {
        Map<PExp, PExp> e = new LinkedHashMap<>();
        e.put(current, replacement);
        return substitute(e);
    }

    public void processStringRepresentation(PExpVisitor visitor, Appendable a) {
        //PExpTextRenderingVisitor renderer = new PExpTextRenderingVisitor(a);
         //PExpVisitor finalVisitor = new NestedPExpVisitors(visitor, renderer);
         //this.accept(finalVisitor);
        //this.accept(renderer);

    }

    public boolean typeMatches(MTType other) {
        return other.isSubtypeOf(getMathType());
    }

    public boolean typeMatches(PExp other) {
        return typeMatches(other.getMathType());
    }

    public abstract void accept(PExpVisitor v);

    public abstract PExp substitute(Map<PExp, PExp> substitutions);

    public abstract boolean containsName(String name);

    public abstract List<? extends PExp> getSubExpressions();

    public abstract boolean isObviouslyTrue();

    public boolean isEquality() {
        return false;
    }

    public abstract boolean isLiteralFalse();

    public abstract boolean isVariable();

    public abstract boolean isLiteral();

    public abstract boolean isFunction();

    /**
     * Converts {@code this} expression, containing an arbitrary number of
     * conjuncts with possibly nested implications, into a list of ((n) antecedent
     * -consequent) pairs. For example, if {@code this} expression is:
     * <pre>
     *     x and y implies z implies a
     * </pre>
     * this method will convert it to {@code x and y and z implies a} by the
     * following rule:
     * <pre>
     *     Confirm (A /\ B) -> C
     *     ------------------------
     *     Confirm A -> B -> C
     * </pre>
     * Similarly, if the expression ends with multiple consequents conjuncted
     * together (e.g.: {@code a and b implies x and y}), then we return a list
     * of all antecedent-grouping and paired with each consequent. For example:
     * <pre>
     *     [a and b implies x,
     *      a and b implies y]
     * </pre>
     *
     * The following simplification rule permits this:
     * <pre>
     *     Confirm (A and ...
     * </pre>
     *
     * @return a list of antecedent - consequent expressions
     */
    public List<PExp> partition() {
        if ( !(this instanceof PSymbol) ) {
            throw new UnsupportedOperationException("no vc partitioning " +
                    "possible for a top level " +
                    this.getClass().getSimpleName() + " expression.");
        }
        TypeGraph g = this.getMathType().getTypeGraph();
        PExp leftSubtree = ((PSymbol) this).getArguments().get(0);
        PExp rightSubtree = ((PSymbol) this).getArguments().get(1);

        List<PExp> result = new ArrayList<>();
        List<PExp> consequents = rightSubtree.splitIntoConjuncts();

        //Todo: Nope, we need a listner for the line below. Needs to split on ands still.
        PExp antecedent = g.formConjuncts(leftSubtree.splitOn("implies"));
        return consequents.stream().map(consequent -> g.formImplies(antecedent, consequent))
                .collect(Collectors.toList());
    }

    public final List<PExp> splitOn(String ... names) {
        return splitOn(Arrays.asList(names));
    }

    public final List<PExp> splitOn(List<String> names) {
        List<PExp> segments = new ArrayList<>();
        splitOn(segments, names);
        return segments;
    }

    protected abstract void splitOn(List<PExp> accumulator, List<String> names);

    public final List<PExp> splitIntoConjuncts() {
        List<PExp> conjuncts = new ArrayList<>();
        splitIntoConjuncts(conjuncts);
        return conjuncts;
    }

    protected abstract void splitIntoConjuncts(List<PExp> accumulator);

    /**
     * Returns a copy of this {@code PExp} where all variables prefixed with
     * an '@' are replaced by just the variable. This is essentially applying
     * the 'remember' vcgen rule.
     * 
     * @return A '@-clean' version of this {@code PExp}.
     */
    public abstract PExp withIncomingSignsErased();

    public abstract PExp withQuantifiersFlipped();

    public final Set<PSymbol> getIncomingVariables() {
        if ( cachedIncomingVariables == null ) {
            cachedIncomingVariables =
                    Collections.unmodifiableSet(getIncomingVariablesNoCache());
        }
        return cachedIncomingVariables;
    }

    public abstract Set<PSymbol> getIncomingVariablesNoCache();

    public final Set<PSymbol> getQuantifiedVariables() {
        if ( cachedQuantifiedVariables == null ) {
            //We're immutable, so only do this once
            cachedQuantifiedVariables =
                    Collections
                            .unmodifiableSet(getQuantifiedVariablesNoCache());
        }
        return cachedQuantifiedVariables;
    }

    public abstract Set<PSymbol> getQuantifiedVariablesNoCache();

    public final List<PExp> getFunctionApplications() {
        if ( cachedFunctionApplications == null ) {
            //We're immutable, so only do this once
            cachedFunctionApplications = getFunctionApplicationsNoCache();
        }
        return cachedFunctionApplications;
    }

    public abstract List<PExp> getFunctionApplicationsNoCache();

    public final Set<String> getSymbolNames() {
        if ( cachedSymbolNames == null ) {
            //We're immutable, so only do this once
            cachedSymbolNames =
                    Collections.unmodifiableSet(getSymbolNamesNoCache());
        }
        return cachedSymbolNames;
    }

    protected abstract Set<String> getSymbolNamesNoCache();

    public static class HashDuple {
        public int structureHash;
        public int valueHash;

        public HashDuple(int structureHash, int valueHash) {
            this.structureHash = structureHash;
            this.valueHash = valueHash;
        }
    }
}
