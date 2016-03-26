package org.rsrg.semantics.programtype;

import org.jetbrains.annotations.NotNull;
import org.rsrg.semantics.DumbTypeGraph;
import org.rsrg.semantics.MathType;
import org.rsrg.semantics.symbol.FacilitySymbol;

import java.util.Map;
import java.util.WeakHashMap;

public class PTInvalid extends ProgType {

    @NotNull private static WeakHashMap<DumbTypeGraph, PTInvalid> instances =
            new WeakHashMap<>();

    @NotNull public static PTInvalid getInstance(@NotNull DumbTypeGraph g) {
        PTInvalid result = instances.get(g);
        if ( result == null ) {
            result = new PTInvalid(g);
            instances.put(g, result);
        }
        return result;
    }

    private PTInvalid(@NotNull DumbTypeGraph g) {
        super(g);
    }

    @NotNull @Override public MathType toMath() {
        return getTypeGraph().INVALID;
    }

    @Override public String toString() {
        return "Invalid";
    }

    @NotNull @Override public ProgType instantiateGenerics(
            @NotNull Map<String, ProgType> genericInstantiations,
            @NotNull FacilitySymbol instantiatingFacility) {
        return this;
    }

}
