package org.rsrg.semantics.programtype;

import org.rsrg.semantics.TypeGraph;
import org.rsrg.semantics.symbol.FacilitySymbol;
import org.rsrg.semantics.MTType;

import java.util.Map;

public abstract class PTType {

    private final TypeGraph typeGraph;

    public PTType(TypeGraph g) {
        this.typeGraph = g;
    }

    public final TypeGraph getTypeGraph() {
        return typeGraph;
    }

    public abstract MTType toMath();

    /**
     * Returns {@code true} if this program type is a 'typed container' of
     * other program types. Basically makes it much easier for us to determine
     * if objects typed with this have members capable of being accessed.
     */
    public boolean isAggregateType() {
        return false;
    }

    public abstract PTType instantiateGenerics(
            Map<String, PTType> genericInstantiations,
            FacilitySymbol instantiatingFacility);

    /**
     * Returns {@code true} <strong>iff</strong> a value of this type
     * would be acceptable where one of type {@code t} were required.
     * 
     * @param t the required type
     * @return {@code true} <strong>iff</strong> an value of this type
     *         would be acceptable where one of type {@code t} were
     *         required
     */
    public boolean acceptableFor(PTType t) {
        return equals(t);
    }
}
