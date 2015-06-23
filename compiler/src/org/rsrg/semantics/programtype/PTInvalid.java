package org.rsrg.semantics.programtype;

import org.rsrg.semantics.MTType;
import org.rsrg.semantics.symbol.FacilitySymbol;
import edu.clemson.resolve.typereasoning.TypeGraph;

import java.util.Map;
import java.util.WeakHashMap;

public class PTInvalid extends PTType {

    private static WeakHashMap<TypeGraph, PTInvalid> instances =
            new WeakHashMap<>();

    public static PTInvalid getInstance(TypeGraph g) {
        PTInvalid result = instances.get(g);
        if ( result == null ) {
            result = new PTInvalid(g);
            instances.put(g, result);
        }
        return result;
    }

    private PTInvalid(TypeGraph g) {
        super(g);
    }

    @Override public MTType toMath() {
        return getTypeGraph().INVALID;
    }

    @Override public String toString() {
        return "Invalid";
    }

    @Override public PTType instantiateGenerics(
            Map<String, PTType> genericInstantiations,
            FacilitySymbol instantiatingFacility) {
        return this;
    }

}