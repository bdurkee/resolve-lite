package org.rsrg.semantics.programtype;

import org.rsrg.semantics.MTType;
import org.rsrg.semantics.symbol.FacilitySymbol;
import org.rsrg.semantics.TypeGraph;

import java.util.Map;

/**
 * The program-type corresponding to TypeGraph.ELEMENT, i.e., the type of
 * all program types.
 */
public class PTElement extends PTType {

    public PTElement(TypeGraph g) {
        super(g);
    }

    @Override public MTType toMath() {
        return getTypeGraph().ELEMENT;
    }

    @Override public PTType instantiateGenerics(
            Map<String, PTType> genericInstantiations,
            FacilitySymbol instantiatingFacility) {
        return this;
    }
}