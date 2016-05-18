package edu.clemson.resolve.semantics.programtype;

import org.jetbrains.annotations.NotNull;
import edu.clemson.resolve.semantics.DumbMathClssftnHandler;
import edu.clemson.resolve.semantics.MathClassification;
import edu.clemson.resolve.semantics.symbol.FacilitySymbol;

import java.util.Map;

public abstract class ProgType {

    @NotNull
    protected final DumbMathClssftnHandler g;

    public ProgType(@NotNull DumbMathClssftnHandler g) {
        this.g = g;
    }

    @NotNull
    public final DumbMathClssftnHandler getTypeGraph() {
        return g;
    }

    @NotNull
    public abstract MathClassification toMath();

    /**
     * Returns {@code true} if this program type is a 'typed container' of other program types. Basically makes it
     * much easier for us to determine
     * if objects typed with this have members capable of being accessed.
     */
    public boolean isAggregateType() {
        return false;
    }

    @NotNull
    public abstract ProgType instantiateGenerics(@NotNull Map<String, ProgType> genericInstantiations,
                                                 @NotNull FacilitySymbol instantiatingFacility);

    /**
     * Returns {@code true} <strong>iff</strong> a value of this type would be acceptable where one of type
     * {@code t} were required.
     *
     * @param t the required type
     * @return {@code true} <strong>iff</strong> an value of this type would be acceptable where one of type
     *      {@code t} were required
     */
    public boolean acceptableFor(@NotNull ProgType t) {
        return equals(t);
    }
}
