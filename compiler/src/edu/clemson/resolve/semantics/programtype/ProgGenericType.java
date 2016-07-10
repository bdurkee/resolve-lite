package edu.clemson.resolve.semantics.programtype;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import edu.clemson.resolve.semantics.MathNamedClassification;
import edu.clemson.resolve.semantics.MathClassification;
import edu.clemson.resolve.semantics.symbol.FacilitySymbol;
import edu.clemson.resolve.semantics.DumbMathClssftnHandler;

import java.util.Map;

public class ProgGenericType extends ProgType {

    @NotNull
    private final String name;

    public ProgGenericType(@NotNull DumbMathClssftnHandler g, @NotNull String name) {
        super(g);
        this.name = name;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    @Override
    public MathClassification toMath() {
        return new MathNamedClassification(getTypeGraph(), name, g.SSET.typeRefDepth - 1, g.SSET);
    }

    @NotNull
    @Override
    public ProgType instantiateGenerics(@NotNull Map<String, ProgType> genericInstantiations,
                                        @NotNull FacilitySymbol instantiatingFacility) {
        ProgType result = this;
        if (genericInstantiations.containsKey(this.name)) {
            result = genericInstantiations.get(this.name);
        }
        return result;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        boolean result = (o instanceof ProgGenericType);

        if (result) {
            ProgGenericType oAsPTGeneric = (ProgGenericType) o;
            result = name.equals(oAsPTGeneric.getName());
        }
        return result;
    }

    @Override
    public String toString() {
        return name;
    }

}