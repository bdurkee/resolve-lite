package org.rsrg.semantics.programtype;

import edu.clemson.resolve.typereasoning.TypeGraph;
import org.rsrg.semantics.MTType;
import org.rsrg.semantics.symbol.FacilitySymbol;

import java.util.Map;
import java.util.WeakHashMap;

public class PTVoid extends PTType {

    private static WeakHashMap<TypeGraph, PTVoid> instances =
            new WeakHashMap<>();

    public static PTVoid getInstance(TypeGraph g) {
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

    private PTVoid(TypeGraph g) {
        super(g);
    }

    @Override public MTType toMath() {
        return getTypeGraph().VOID;
    }

    @Override public boolean equals(Object o) {
        //We override this simply to show that we've given it some thought
        return super.equals(o);
    }

    @Override public PTType instantiateGenerics(
            Map<String, PTType> genericInstantiations,
            FacilitySymbol instantiatingFacility) {
        return this;
    }
}
