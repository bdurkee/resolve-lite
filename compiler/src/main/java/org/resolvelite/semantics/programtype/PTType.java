package org.resolvelite.semantics.programtype;

import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.semantics.MTType;
import org.resolvelite.typereasoning.TypeGraph;

import java.util.Map;

public abstract class PTType {

    private final TypeGraph typeGraph;
    private final String name;

    public PTType(TypeGraph g, String name) {
        this.typeGraph = g;
        this.name = name;
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
