package org.resolvelite.semantics.programtype;

import org.resolvelite.semantics.MTType;
import org.resolvelite.typereasoning.TypeGraph;

import java.util.WeakHashMap;

public class PTVoid extends PTType {

    private static WeakHashMap<TypeGraph, PTVoid> instances =
            new WeakHashMap<TypeGraph, PTVoid>();

    public static PTVoid getInstance(TypeGraph g) {
        PTVoid result = instances.get(g);
        if ( result == null ) {
            result = new PTVoid(g);
            instances.put(g, result);
        }
        return result;
    }

    private PTVoid(TypeGraph g) {
        super(g);
    }

    @Override public MTType toMath() {
        return getTypeGraph().VOID;
    }
}
