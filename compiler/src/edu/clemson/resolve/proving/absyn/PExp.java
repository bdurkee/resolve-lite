package edu.clemson.resolve.proving.absyn;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rsrg.semantics.MTType;
import org.rsrg.semantics.programtype.PTType;

import java.util.*;

public abstract class PExp {

    public final int structureHash;
    public final int valueHash;
    private final MTType type, typeValue;

    /** Since the removal of the Exp hierarchy, the role of PExps has expanded
     *  considerably.
     *  <p>
     *  In other words, if this {@code PExp} was born out of a
     *  programmatic expression (for vcgen), program type info should be
     *  present, if not, then these should/will be {@code null}.</p>
     */
    private final PTType progType, progTypeValue;

    private Set<String> cachedSymbolNames = null;
    private List<PExp> cachedFunctionApplications = null;
    private Set<PSymbol> cachedQuantifiedVariables = null;
    private Set<PSymbol> cachedIncomingVariables = null;

    public PExp(@NotNull PSymbol.HashDuple hashes, @NotNull MTType type,
                @Nullable MTType typeValue) {
        this(hashes.structureHash, hashes.valueHash, type, typeValue, null,
                null);
    }

    public PExp(@NotNull PSymbol.HashDuple hashes, @NotNull MTType type,
                @Nullable MTType typeValue, @Nullable PTType progType,
                @Nullable PTType progTypeValue) {
        this(hashes.structureHash, hashes.valueHash, type, typeValue, progType,
                progTypeValue);
    }

    public PExp(int structureHash, int valueHash, @NotNull MTType type,
                @Nullable MTType typeValue) {
        this(structureHash, valueHash, type, typeValue, null, null);
    }

    public PExp(int structureHash, int valueHash, @NotNull MTType type,
                @Nullable MTType typeValue, @Nullable PTType progType,
                @Nullable PTType progTypeValue) {
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

    @Nullable public final PTType getProgType() {
        return progType;
    }

    @Nullable public final PTType getProgTypeValue() {
        return progTypeValue;
    }

    @NotNull public final MTType getMathType() {
        return type;
    }

    @Nullable public final MTType getMathTypeValue() {
        return typeValue;
    }

    @NotNull public PExp substitute(List<? extends PExp> currents, PExp repl) {
        Map<PExp, PExp> substitutions = new HashMap<>();
        for (PExp current : currents) {
            substitutions.put(current, repl);
        }
        return substitute(substitutions);
    }

    @NotNull public PExp substitute(@NotNull List<? extends PExp> currents,
                                    @NotNull PExp... repls) {
        return substitute(currents, Arrays.asList(repls));
    }

    @NotNull public PExp substitute(@NotNull List<? extends PExp> currents,
                                    @NotNull List<? extends PExp> repls) {
        if (currents.size() != repls.size()) {
            throw new IllegalArgumentException("substitution lists must be"
                    + "the same length");
        }
        Iterator<? extends PExp> replIter = repls.iterator();
        Iterator<? extends PExp> currIter = currents.iterator();
        Map<PExp, PExp> result = new LinkedHashMap<>();
        while (replIter.hasNext()) {
            result.put(currIter.next(), replIter.next());
        }
        return substitute(result);
    }

    public boolean staysSameAfterSubstitution(Map<PExp, PExp> substitutions) {
        PExp thisSubstituted = substitute(substitutions);
        return this.equals(thisSubstituted);
    }

    @NotNull public PExp substitute(PExp current, PExp replacement) {
        Map<PExp, PExp> e = new LinkedHashMap<>();
        e.put(current, replacement);
        return substitute(e);
    }

    public boolean typeMatches(MTType other) {
        return other.isSubtypeOf(getMathType());
    }

    public boolean typeMatches(PExp other) {
        return typeMatches(other.getMathType());
    }

    public abstract void accept(PExpListener v);

    @NotNull public abstract PExp substitute(
            @NotNull Map<PExp, PExp> substitutions);

    public abstract boolean containsName(String name);

    @NotNull public abstract List<? extends PExp> getSubExpressions();

    /**
     * A predicate that returns {@code true} in any of the following cases:
     *
     * <ul>
     *     <li>If we're an instance of {@code PSymbol} whose name is simply
     *     {@code true}.</li>
     *     <li>If we're an expression whose top level is a binary application
     *     of the {@code =}s operator whose left and right arguments are
     *     themselves equal (as determined via a call to {@link PExp#equals(Object)}).</li>
     * </ul>
     * @return whether or not we represent a trivially 'true' expression
     */
    public boolean isObviouslyTrue() {
        return false;
    }

    /**
     * Returns {@code true} if this {@code PExp} represents a primitive
     * application of the {@code =} (equals) operator; {@code false} otherwise.
     *
     * @return whether or not we have represent a top-level application of
     *         equals
     */
    public boolean isEquality() {
        return false;
    }

    public boolean isLiteralFalse() {
        return false;
    }

    public boolean isVariable() {
        return false;
    }

    /**
     * If this {@code PExp} is one with a sensible (e.g. extant) name,
     * then this method simply returns it, independent of any parens or other
     * syntactic characteristics.
     *
     * <p>If {@code this} is anonoymous, then we simply return a canned string
     * such as {@code \:lambda}.</p>
     *
     * <p>
     * However, if your dealing with an anonymous application, the way this
     * method is currently implemented; it will recursively descend into the
     * anonymous name portion and bring back the leaf name.
     * For example, say we call this on the following {@code PExp}:
     *
     * <pre>
     *     SS(k)(Cen(k))</pre>
     * <p>
     *
     * @return
     */
    @NotNull protected abstract String getCanonicalName();

    public boolean isLiteral() {
        return false;
    }

    public abstract boolean isFunctionApplication();

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
    @NotNull public List<PExp> experimentalSplit() {
        return experimentalSplit(getMathType().getTypeGraph().getTrueExp());
    }

    @NotNull protected List<PExp> experimentalSplit(PExp assumtions) {
        return new ArrayList<>();
    }

    @NotNull public final List<PExp> splitIntoConjuncts() {
        List<PExp> conjuncts = new ArrayList<>();
        splitIntoConjuncts(conjuncts);
        return conjuncts;
    }

    protected abstract void splitIntoConjuncts(@NotNull List<PExp> accumulator);

    /**
     * Returns a copy of this {@code PExp} where all variables prefixed with
     * an '@' are replaced by just the variable. This is essentially applying
     * the 'remember' vcgen rule.
     * 
     * @return A '@-clean' version of this {@code PExp}.
     */
    @NotNull public abstract PExp withIncomingSignsErased();

    @NotNull public abstract PExp withQuantifiersFlipped();

    /**
     * Returns the set of '@'-prefixed symbols appearing in this {@code PExp}
     * expression. Note that when we say 'symbols' we mean both function
     * applications and argument-less variables.
     *
     * @return the set of all incoming variable symbols
     */
    @NotNull public final Set<PSymbol> getIncomingVariables() {
        if ( cachedIncomingVariables == null ) {
            cachedIncomingVariables = Collections.unmodifiableSet(
                            getIncomingVariablesNoCache());
        }
        return cachedIncomingVariables;
    }

    @NotNull public abstract Set<PSymbol> getIncomingVariablesNoCache();

    @NotNull public final Set<PSymbol> getQuantifiedVariables() {
        if ( cachedQuantifiedVariables == null ) {
            //We're immutable, so only do this once
            cachedQuantifiedVariables =
                    Collections
                            .unmodifiableSet(getQuantifiedVariablesNoCache());
        }
        return cachedQuantifiedVariables;
    }

    @NotNull public abstract Set<PSymbol> getQuantifiedVariablesNoCache();

    @NotNull public final List<PExp> getFunctionApplications() {
        if ( cachedFunctionApplications == null ) {
            //We're immutable, so only do this once
            cachedFunctionApplications = getFunctionApplicationsNoCache();
        }
        return cachedFunctionApplications;
    }

    @NotNull public abstract List<PExp> getFunctionApplicationsNoCache();

    @NotNull public final Set<String> getSymbolNames(boolean excludeApplications,
                                            boolean excludeLiterals) {
        return getSymbolNamesNoCache();
    }

    @NotNull public final Set<String> getSymbolNames() {
        return getSymbolNames(false, false);
    }

    //force implementation of equals for every subclass.
    @Override public abstract boolean equals(Object o);

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
