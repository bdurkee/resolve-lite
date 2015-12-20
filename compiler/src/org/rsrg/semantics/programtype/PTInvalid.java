package org.rsrg.semantics.programtype;

import org.jetbrains.annotations.NotNull;
import org.rsrg.semantics.MTType;
import org.rsrg.semantics.symbol.FacilitySymbol;
import org.rsrg.semantics.TypeGraph;

import java.util.Map;
import java.util.WeakHashMap;

public class PTInvalid extends PTType {

    @NotNull private static WeakHashMap<TypeGraph, PTInvalid> instances =
            new WeakHashMap<>();

    @NotNull public static PTInvalid getInstance(@NotNull TypeGraph g) {
        PTInvalid result = instances.get(g);
        if ( result == null ) {
            result = new PTInvalid(g);
            instances.put(g, result);
        }
        return result;
    }

    private PTInvalid(@NotNull TypeGraph g) {
        super(g);
    }

    @NotNull @Override public MTType toMath() {
        return getTypeGraph().INVALID;
    }

    @Override public String toString() {
        return "Invalid";
    }

    @NotNull @Override public PTType instantiateGenerics(
            @NotNull Map<String, PTType> genericInstantiations,
            @NotNull FacilitySymbol instantiatingFacility) {
        return this;
    }

}
