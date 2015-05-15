package org.resolvelite.semantics.programtype;

import org.resolvelite.semantics.MTType;
import org.resolvelite.semantics.TypeGraph;

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
}
