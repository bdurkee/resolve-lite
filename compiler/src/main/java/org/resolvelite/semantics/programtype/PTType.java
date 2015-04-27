package org.resolvelite.semantics.programtype;

import org.resolvelite.semantics.MTType;
import org.resolvelite.typereasoning.TypeGraph;

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

    // public abstract PTType instantiateGenerics(
    //         Map<String, PTType> genericInstantiations,
    //         FacilityEntry instantiatingFacility);
}
