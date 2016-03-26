package org.rsrg.semantics;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class MathInvalidType extends MathType {

    private static WeakHashMap<DumbTypeGraph, MathInvalidType> instances =
            new WeakHashMap<>();

    public static MathInvalidType getInstance(DumbTypeGraph g) {
        MathInvalidType result = instances.get(g);
        if ( result == null ) {
            result = new MathInvalidType(g);
            instances.put(g, result);
        }
        return result;
    }

    public String getName() {
        return "Invalid";
    }

    private MathInvalidType(DumbTypeGraph g) {
        super(g, null);
    }

    @Override public MathType getEnclosingType() {
        return g.INVALID;
    }

    @Override public MathType withVariablesSubstituted(
            Map<MathType, MathType> substitutions) {
        return this;
    }

    @Override public String toString() {
        return "INVD_MATH_TYPE";
    }

}
