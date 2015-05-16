package org.resolvelite.semantics.programtype;

import org.resolvelite.semantics.MTType;
import org.resolvelite.typereasoning.TypeGraph;

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

    /**
     * Returns {@code true} <strong>iff</strong> an value of this type
     * would be acceptable where one of type {@code t} were required.</p>
     * 
     * @param t The required type.
     * 
     * @return {@code true} <strong>iff</strong> an value of this type
     *         would be acceptable where one of type {@code t} were
     *         required.
     */
    public boolean acceptableFor(PTType t) {
        return equals(t);
    }
}
