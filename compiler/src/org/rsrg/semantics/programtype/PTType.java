package org.rsrg.semantics.programtype;

import org.jetbrains.annotations.NotNull;
import org.rsrg.semantics.TypeGraph;
import org.rsrg.semantics.symbol.FacilitySymbol;
import org.rsrg.semantics.MTType;

import java.util.Map;

public abstract class PTType {

    @NotNull private final TypeGraph typeGraph;

    public PTType(@NotNull TypeGraph g) {
        this.typeGraph = g;
    }

    @NotNull public final TypeGraph getTypeGraph() {
        return typeGraph;
    }

    @NotNull public abstract MTType toMath();

    /**
     * Returns {@code true} if this program type is a 'typed container' of
     * other program types. Basically makes it much easier for us to determine
     * if objects typed with this have members capable of being accessed.
     */
    public boolean isAggregateType() {
        return false;
    }

    @NotNull public abstract PTType instantiateGenerics(
            @NotNull Map<String, PTType> genericInstantiations,
            @NotNull FacilitySymbol instantiatingFacility);

    /**
     * Returns {@code true} <strong>iff</strong> a value of this type
     * would be acceptable where one of type {@code t} were required.
     * 
     * @param t the required type
     * @return {@code true} <strong>iff</strong> an value of this type
     *         would be acceptable where one of type {@code t} were
     *         required
     */
    public boolean acceptableFor(@NotNull PTType t) {
        return equals(t);
    }
}
