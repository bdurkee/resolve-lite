package org.resolvelite.semantics;

import org.resolvelite.typereasoning.TypeGraph;

import java.util.Collections;
import java.util.List;
import java.util.WeakHashMap;

public class MTInvalid extends MTType {

    private static WeakHashMap<TypeGraph, MTInvalid> instances =
            new WeakHashMap<>();

    public static MTInvalid getInstance(TypeGraph g) {
        MTInvalid result = instances.get(g);
        if ( result == null ) {
            result = new MTInvalid(g);
            instances.put(g, result);
        }
        return result;
    }

    public String getName() {
        return "Invalid";
    }

    private MTInvalid(TypeGraph g) {
        super(g);
    }

    @Override public List<? extends MTType> getComponentTypes() {
        return Collections.emptyList();
    }
}
