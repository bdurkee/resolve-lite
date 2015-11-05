package edu.clemson.resolve.proving.absyn;

import edu.clemson.resolve.misc.Utils;
import org.jetbrains.annotations.NotNull;
import org.rsrg.semantics.MTType;

import java.util.*;
import java.util.stream.Collectors;

public class PSet extends PExp {

    //Todo: should this be a set?
    private final List<PExp> elements = new ArrayList<>();

    //TODO: Give me a real HashDuple (one based on my actual elements!)
    public PSet(MTType type, MTType typeValue, List<PExp> elements) {
        super(new HashDuple(0, 56), type, typeValue);
        this.elements.addAll(elements);
    }

    @Override public void accept(PExpListener v) {
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

    @NotNull @Override public PExp substitute(@NotNull Map<PExp, PExp> substitutions) {
        return new PSet(getMathType(), getMathTypeValue(),
                Utils.apply(elements, u -> u.substitute(substitutions)));
    }

    @Override public boolean containsName(String name) {
        return elements.stream()
                .filter(u -> u.containsName(name))
                .collect(Collectors.toList()).isEmpty();
    }

    @NotNull @Override public List<? extends PExp> getSubExpressions() {
        return elements;
    }

    @Override public boolean isObviouslyTrue() {
        return false;
    }

    @Override public boolean isLiteralFalse() {
        return false;
    }

    @Override public boolean isVariable() {
        return false;
    }

    @NotNull @Override protected String getCanonicalName() {
        return "{ PSet }";
    }

    @Override public boolean isLiteral() {
        return false;
    }

    @Override public boolean isFunctionApplication() {
        return false;
    }

    @Override protected void splitIntoConjuncts(@NotNull List<PExp> accumulator) {}

    @NotNull @Override public PExp withIncomingSignsErased() {
        return new PSet(getMathType(), getMathTypeValue(),
                Utils.apply(elements, PExp::withIncomingSignsErased));
    }

    @NotNull @Override public PExp withQuantifiersFlipped() {
        return null;
    }

    @NotNull @Override public Set<PSymbol> getIncomingVariablesNoCache() {
        return new LinkedHashSet<>();
    }

    @NotNull @Override public Set<PSymbol> getQuantifiedVariablesNoCache() {
        return new HashSet<>();
    }

    @NotNull @Override public List<PExp> getFunctionApplicationsNoCache() {
        return new ArrayList<>();
    }

    @Override protected Set<String> getSymbolNamesNoCache() {
        return new HashSet<>();
    }

    @Override public String toString() {
        return "{" + Utils.join(elements, ", ") + "}";
    }
}