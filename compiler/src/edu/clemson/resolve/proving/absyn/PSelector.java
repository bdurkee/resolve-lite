package edu.clemson.resolve.proving.absyn;

import org.antlr.v4.runtime.Token;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/** Represents a tree-like {@code .}-segmented list of field accesses */
//TODO: Determine if (somehow) this should just be folded into {@link PApply}.
public class PSelector extends PExp {

    private final PExp left, right;

    public PSelector(@NotNull PExp left, @NotNull PExp right) {
        super(left.structureHash * 72, right.structureHash * 36, right.getMathClssftn(), right.getProgType());
        this.left = left;
        this.right = right;
    }

    public PSelector(@NotNull PExp left,
                     @NotNull PExp right,
                     @Nullable Token vcLocation,
                     @Nullable String vcExplanation) {
        super(left.structureHash * 72, right.structureHash * 36, right.getMathClssftn(),
                right.getProgType(), vcLocation, vcExplanation);
        this.left = left;
        this.right = right;
    }

    @Override
    public void accept(PExpListener v) {
        v.beginPExp(this);
        v.beginPSelector(this);
        v.beginChildren(this);

        left.accept(v);
        right.accept(v);

        v.endChildren(this);
        v.endPSelector(this);
        v.endPExp(this);
    }

    @NotNull
    @Override
    public PExp substitute(@NotNull Map<PExp, PExp> substitutions) {
        PExp result;
        if (substitutions.containsKey(this)) {
            result = substitutions.get(this);
        }
        else {
            result = new PSelector(left.substitute(substitutions), right.substitute(substitutions));
        }
        return result;
    }

    @Override
    public boolean isIncoming() {
        return left.isIncoming();
    }

    @Override
    public boolean isVariable() {
        return right.isVariable();
    }

    @Override
    public boolean containsName(String name) {
        return left.containsName(name) || right.containsName(name);
    }

    @NotNull
    @Override
    public List<? extends PExp> getSubExpressions() {
        List<PExp> result = new ArrayList<>();
        result.add(left);
        result.add(right);
        return result;
    }

    @NotNull
    @Override
    protected String getCanonicalName() {
        return left.getCanonicalName() + "." + right.getCanonicalName();
    }

    @Override
    protected void splitIntoConjuncts(@NotNull List<PExp> accumulator) {
        accumulator.add(this);
    }

    @Override
    public PExp withVCInfo(@Nullable Token location, @Nullable String explanation) {
        return new PSelector(left, right, location, explanation);
    }

    @NotNull
    @Override
    public PExp withIncomingSignsErased() {
        return new PSelector(left.withIncomingSignsErased(), right.withIncomingSignsErased());
    }

    //shouldn't be any quantifiers in a dot expr
    @NotNull
    @Override
    public PExp withQuantifiersFlipped() {
        return this;
    }

    //TODO: Someday, if this class is still around, use Utils.apply (collection ver. here)
    @NotNull
    @Override
    public Set<PSymbol> getIncomingVariablesNoCache() {
        Set<PSymbol> result = new LinkedHashSet<>(left.getIncomingVariables());
        result.addAll(right.getIncomingVariables());
        return result;
    }

    @NotNull
    @Override
    public Set<PSymbol> getQuantifiedVariablesNoCache() {
        Set<PSymbol> result = new LinkedHashSet<>(left.getQuantifiedVariables());
        result.addAll(right.getQuantifiedVariables());
        return result;
    }

    @NotNull
    @Override
    public List<PExp> getFunctionApplicationsNoCache() {
        List<PExp> result = new LinkedList<>(left.getFunctionApplications());
        result.addAll(right.getFunctionApplications());
        return result;
    }

    //TODO: I'm confused. Why, in dot expressions (i call these "selector exprs"), do we not consider the
    //names of individual segments when performing the parsimonious step?
    //For instance, say I have:
    //      Assume conc.P.Trmn_Loc = x;
    //      Confirm conc.P.Curr_Loc = y;
    //The intersection of the assume and confirm in this case is the empty set.
    //But why? If we're representing dot exps as a tree
    //(which is what they really are) then "P" and "conc" should be in the
    // intersection right? Why is it we only consider the entire string? Is
    // there some mathematical justification for that?
    @Override
    protected Set<String> getSymbolNamesNoCache(boolean excludeApplications, boolean excludeLiterals) {
        Set<String> result = new LinkedHashSet<>();
        result.add(this.getCanonicalName());
        return result;
        /*Set<String> result =
                new LinkedHashSet<>(left.getSymbolNames(
                        excludeApplications, excludeLiterals));
        result.addAll(right.getSymbolNames(
                excludeApplications, excludeLiterals));
        return result;*/
    }

    @Override
    public boolean equals(Object o) {
        boolean result = (o instanceof PSelector);
        if (result) {
            result = left.equals(((PSelector) o).left) && right.equals(((PSelector) o).right);
        }
        return result;
    }

    @Override
    public String toString() {
        return left + "." + right;
    }
}
