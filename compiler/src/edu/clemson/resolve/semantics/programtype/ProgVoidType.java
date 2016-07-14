package edu.clemson.resolve.semantics.programtype;

import org.jetbrains.annotations.NotNull;
import edu.clemson.resolve.semantics.DumbMathClssftnHandler;
import edu.clemson.resolve.semantics.MathClssftn;
import edu.clemson.resolve.semantics.symbol.FacilitySymbol;

import java.util.Map;
import java.util.WeakHashMap;

public class ProgVoidType extends ProgType {

    @NotNull
    private static WeakHashMap<DumbMathClssftnHandler, ProgVoidType> instances = new WeakHashMap<>();

    @NotNull
    public static ProgVoidType getInstance(@NotNull DumbMathClssftnHandler g) {
        ProgVoidType result = instances.get(g);
        if (result == null) {
            result = new ProgVoidType(g);
            instances.put(g, result);
        }
        return result;
    }

    @Override
    public String toString() {
        return "Void";
    }

    private ProgVoidType(@NotNull DumbMathClssftnHandler g) {
        super(g);
    }

    @NotNull
    @Override
    public MathClssftn toMath() {
        return getTypeGraph().VOID;
    }

    @Override
    public boolean equals(Object o) {
        //We override this simply to show that we've given it some thought
        return super.equals(o);
    }

    @NotNull
    @Override
    public ProgType instantiateGenerics(@NotNull Map<String, ProgType> genericInstantiations,
                                        @NotNull FacilitySymbol instantiatingFacility) {
        return this;
    }
}
