package edu.clemson.resolve.proving.absyn;

import edu.clemson.resolve.misc.Utils;
import org.rsrg.semantics.MTType;

import java.util.*;
import java.util.stream.Collectors;

public class PSet extends PExp {

    //Todo: should this be a set?
    private final List<PExp> elements = new ArrayList<>();

    public PSet(MTType type, MTType typeValue, List<PExp> elements) {
        super(PSymbol.calculateHashes(elements), type, typeValue);
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

    @Override public PExp substitute(Map<PExp, PExp> substitutions) {
        return new PSet(getMathType(), getMathTypeValue(),
                Utils.apply(elements, u -> u.substitute(substitutions)));
    }

    @Override public boolean containsName(String name) {
        return elements.stream()
                .filter(u -> u.containsName(name))
                .collect(Collectors.toList()).isEmpty();
    }

    @Override public List<? extends PExp> getSubExpressions() {
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

    @Override public boolean isLiteral() {
        return false;
    }

    @Override public boolean isFunctionApplication() {
        return false;
    }

    @Override protected void splitIntoConjuncts(List<PExp> accumulator) {}

    @Override public PExp withIncomingSignsErased() {
        return new PSet(getMathType(), getMathTypeValue(),
                Utils.apply(elements, PExp::withIncomingSignsErased));
    }

    @Override public PExp withQuantifiersFlipped() {
        return null;
    }

    @Override public Set<PSymbol> getIncomingVariablesNoCache() {
        return new LinkedHashSet<>();
    }

    @Override public Set<PSymbol> getQuantifiedVariablesNoCache() {
        return new HashSet<>();
    }

    @Override public List<PExp> getFunctionApplicationsNoCache() {
        return new ArrayList<>();
    }

    @Override protected Set<String> getSymbolNamesNoCache(boolean includeApplications, boolean excludeLiterals) {
        return new HashSet<>();
    }

    @Override public String toString() {
        return "{" + Utils.join(elements, ", ") + "}";
    }
}