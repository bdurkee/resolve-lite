package org.rsrg.semantics.programtype;

import org.jetbrains.annotations.NotNull;
import org.rsrg.semantics.DumbTypeGraph;
import org.rsrg.semantics.MathType;
import org.rsrg.semantics.symbol.FacilitySymbol;

import java.util.Map;
import java.util.WeakHashMap;

public class PTVoid extends ProgType {

    @NotNull private static WeakHashMap<DumbTypeGraph, PTVoid> instances =
            new WeakHashMap<>();

    @NotNull public static PTVoid getInstance(@NotNull DumbTypeGraph g) {
        PTVoid result = instances.get(g);
        if ( result == null ) {
            result = new PTVoid(g);
            instances.put(g, result);
        }
        return result;
    }

    @Override public String toString() {
        return "Void";
    }

    private PTVoid(@NotNull DumbTypeGraph g) {
        super(g);
    }

    @NotNull @Override public MathType toMath() {
        return getTypeGraph().VOID;
    }

    @Override public boolean equals(Object o) {
        //We override this simply to show that we've given it some thought
        return super.equals(o);
    }

    @NotNull @Override public ProgType instantiateGenerics(
            @NotNull Map<String, ProgType> genericInstantiations,
            @NotNull FacilitySymbol instantiatingFacility) {
        return this;
    }
}
