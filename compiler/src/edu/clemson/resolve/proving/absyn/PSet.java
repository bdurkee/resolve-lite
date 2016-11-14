package edu.clemson.resolve.proving.absyn;

import edu.clemson.resolve.misc.Utils;
import org.antlr.v4.runtime.Token;
import org.jetbrains.annotations.NotNull;
import edu.clemson.resolve.semantics.MathClssftn;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class PSet extends PExp {

    //Todo: should this be a set?
    private final List<PExp> elements = new ArrayList<>();

    //TODO: Give me a real HashDuple (one based on my actual elements!)
    public PSet(@NotNull MathClssftn type, @NotNull List<PExp> elements) {
        super(new HashDuple(0, 56), type);
        this.elements.addAll(elements);
    }

    public PSet(@NotNull MathClssftn type,
                @NotNull List<PExp> elements,
                @Nullable Token vcLocation,
                @Nullable String vcExplanation) {
        super(new HashDuple(0, 56), type, null, vcLocation, vcExplanation);
        this.elements.addAll(elements);
    }

    @Override
    public PExp withPrimeMarkAdded() {
        return new PSet(getMathClssftn(), elements, getVCLocation(), getVCExplanation());
    }

    @Override
    public void accept(PExpListener v) {
        v.beginPExp(this);
        v.beginPSet(this);
        v.beginChildren(this);
        boolean first = true;

        for (PExp e : elements) {
            if (!first) {
                v.fencepostPSet(this);
            }
            first = false;
            e.accept(v);
        }
        v.endChildren(this);
        v.endPSet(this);
        v.endPExp(this);
    }

    @NotNull
    @Override
    public PExp substitute(@NotNull Map<PExp, PExp> substitutions) {
        return new PSet(getMathClssftn(), Utils.apply(elements, u -> u.substitute(substitutions)));
    }

    @Override
    public boolean containsName(String name) {
        for (PExp e : elements) {
            if (e.containsName(name)) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    @Override
    public List<? extends PExp> getSubExpressions() {
        return elements;
    }

    @Override
    public boolean isObviouslyTrue() {
        return false;
    }

    @Override
    public boolean isLiteralFalse() {
        return false;
    }

    @Override
    public boolean isVariable() {
        return false;
    }

    @NotNull
    @Override
    public String getTopLevelOperationName() {
        return "{ PSet }";
    }

    @Override
    public boolean isLiteral() {
        return false;
    }

    @Override
    public boolean isFunctionApplication() {
        return false;
    }

    @Override
    protected void splitIntoConjuncts(@NotNull List<PExp> accumulator) {
    }

    @Override
    public PExp withVCInfo(@Nullable Token location, @Nullable String explanation) {
        return null;
    }

    @NotNull
    @Override
    public PExp withIncomingSignsErased() {
        return new PSet(getMathClssftn(),
                Utils.apply(elements, PExp::withIncomingSignsErased));
    }

    @NotNull
    @Override
    public PExp withQuantifiersFlipped() {
        return null;
    }

    @NotNull
    @Override
    public Set<PSymbol> getIncomingVariablesNoCache() {
        return new LinkedHashSet<>();
    }

    @NotNull
    @Override
    public Set<PSymbol> getQuantifiedVariablesNoCache() {
        return new HashSet<>();
    }

    @NotNull
    @Override
    public Set<PSymbol> getFreeVariablesNoCache() {
        return new HashSet<>();
    }

    @NotNull
    @Override
    public List<PExp> getFunctionApplicationsNoCache() {
        return new ArrayList<>();
    }

    @Override
    public boolean equals(Object o) {
        return false;
    }

    @Override
    protected Set<String> getSymbolNamesNoCache(boolean excludeApplications, boolean excludeLiterals) {
        return new HashSet<>();
    }

    @Override
    public String toString() {
        return "{" + Utils.join(elements, ", ") + "}";
    }
}