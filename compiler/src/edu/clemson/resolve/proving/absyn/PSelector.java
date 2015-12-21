package edu.clemson.resolve.proving.absyn;

import edu.clemson.resolve.misc.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rsrg.semantics.MTType;

import java.util.*;
import java.util.function.Function;

/** Represents a tree-like {@code .}-segmented list of field accesses */
//TODO: Determine if (somehow) this should just be folded into {@link PApply}.
public class PSelector extends PExp {

    private final PExp left, right;

    public PSelector(@NotNull PExp left, @NotNull PExp right) {
        super(left.structureHash * 72, right.structureHash * 36,
                right.getMathType(), right.getMathTypeValue());
        this.left = left;
        this.right = right;
    }

    @Override public void accept(PExpListener v) {
    }

    @NotNull @Override public PExp substitute(
            @NotNull Map<PExp, PExp> substitutions) {
        return new PSelector(left.substitute(substitutions),
                right.substitute(substitutions));
    }

    @Override public boolean isIncoming() {
        return left.isIncoming();
    }

    @Override public boolean containsName(String name) {
        return left.containsName(name) || right.containsName(name);
    }

    @NotNull @Override public List<? extends PExp> getSubExpressions() {
        List<PExp> result = new ArrayList<>();
        result.add(left);
        result.add(right);
        return result;
    }

    @NotNull @Override protected String getCanonicalName() {
        return right.getCanonicalName();
    }

    @Override protected void splitIntoConjuncts(
            @NotNull List<PExp> accumulator) {
        accumulator.add(this);
    }

    @NotNull @Override public PExp withIncomingSignsErased() {
        return new PSelector(left.withIncomingSignsErased(),
                right.withIncomingSignsErased());
    }

    //shouldn't be any quantifiers in a dot expr
    @NotNull @Override public PExp withQuantifiersFlipped() {
        return this;
    }

    //TODO: Someday, if this class is still around, use Utils.apply (collection ver. here)
    @NotNull @Override public Set<PSymbol> getIncomingVariablesNoCache() {
        Set<PSymbol> result =
                new LinkedHashSet<>(left.getIncomingVariables());
        result.addAll(right.getIncomingVariables());
        return result;
    }

    @NotNull @Override public Set<PSymbol> getQuantifiedVariablesNoCache() {
        Set<PSymbol> result =
                new LinkedHashSet<>(left.getQuantifiedVariables());
        result.addAll(right.getQuantifiedVariables());
        return result;
    }

    @NotNull @Override public List<PExp> getFunctionApplicationsNoCache() {
        List<PExp> result =
                new LinkedList<>(left.getFunctionApplications());
        result.addAll(right.getFunctionApplications());
        return result;
    }

    @Override protected Set<String> getSymbolNamesNoCache(
            boolean excludeApplications, boolean excludeLiterals) {
        Set<String> result =
                new LinkedHashSet<>(left.getSymbolNames(
                        excludeApplications, excludeLiterals));
        result.addAll(right.getSymbolNames(
                excludeApplications, excludeLiterals));
        return result;
    }

    @Override public boolean equals(Object o) {
        boolean result = (o instanceof PSelector);
        if (result) {
            result = left.equals(((PSelector)o).left) &&
                    right.equals(((PSelector)o).right);
        }
        return result;
    }

    @Override public String toString() {
        return left + "." + right;
    }
}
