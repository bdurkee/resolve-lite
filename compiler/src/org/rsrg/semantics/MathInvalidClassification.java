package org.rsrg.semantics;

import java.util.Map;
import java.util.WeakHashMap;

public class MathInvalidClassification extends MathClassification {

    private static WeakHashMap<DumbTypeGraph, MathInvalidClassification> instances =
            new WeakHashMap<>();

    public static MathInvalidClassification getInstance(DumbTypeGraph g) {
        MathInvalidClassification result = instances.get(g);
        if ( result == null ) {
            result = new MathInvalidClassification(g);
            instances.put(g, result);
        }
        return result;
    }

    public String getName() {
        return "Invalid";
    }

    private MathInvalidClassification(DumbTypeGraph g) {
        super(g, null);
    }

    @Override public MathClassification getEnclosingClassification() {
        return g.INVALID;
    }

    @Override public MathClassification withVariablesSubstituted(
            Map<MathClassification, MathClassification> substitutions) {
        return this;
    }

    @Override public String toString() {
        return "INVD_MATH_TYPE";
    }

}
