package org.rsrg.semantics.programtype;

import org.jetbrains.annotations.NotNull;
import org.rsrg.semantics.TypeGraph;
import org.rsrg.semantics.MTType;
import org.rsrg.semantics.symbol.FacilitySymbol;

import java.util.Map;
import java.util.WeakHashMap;

public class PTVoid extends PTType {

    @NotNull private static WeakHashMap<TypeGraph, PTVoid> instances =
            new WeakHashMap<>();

    @NotNull public static PTVoid getInstance(@NotNull TypeGraph g) {
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

    private PTVoid(@NotNull TypeGraph g) {
        super(g);
    }

    @NotNull @Override public MTType toMath() {
        return getTypeGraph().VOID;
    }

    @Override public boolean equals(Object o) {
        //We override this simply to show that we've given it some thought
        return super.equals(o);
    }

    @NotNull @Override public PTType instantiateGenerics(
            @NotNull Map<String, PTType> genericInstantiations,
            @NotNull FacilitySymbol instantiatingFacility) {
        return this;
    }
}
