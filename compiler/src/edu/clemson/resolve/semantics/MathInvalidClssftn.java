package edu.clemson.resolve.semantics;

import java.util.Map;
import java.util.WeakHashMap;

public class MathInvalidClssftn extends MathClssftn {

    private static WeakHashMap<DumbMathClssftnHandler, MathInvalidClssftn> instances = new WeakHashMap<>();

    public static MathInvalidClssftn getInstance(DumbMathClssftnHandler g) {
        MathInvalidClssftn result = instances.get(g);
        if (result == null) {
            result = new MathInvalidClssftn(g);
            instances.put(g, result);
        }
        return result;
    }

    public String getName() {
        return "Invalid";
    }

    private MathInvalidClssftn(DumbMathClssftnHandler g) {
        super(g, null);
    }

    @Override
    public MathClssftn getEnclosingClassification() {
        return g.INVALID;
    }

    @Override
    public MathClssftn withVariablesSubstituted(Map<String, MathClssftn> substitutions) {
        return this;
    }

    @Override
    public String toString() {
        return "INVD_MATH_TYPE";
    }

}
