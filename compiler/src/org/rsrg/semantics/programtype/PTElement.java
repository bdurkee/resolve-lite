package org.rsrg.semantics.programtype;

import org.jetbrains.annotations.NotNull;
import org.rsrg.semantics.MTType;
import org.rsrg.semantics.symbol.FacilitySymbol;
import org.rsrg.semantics.TypeGraph;

import java.util.Map;

/**
 * The program-type corresponding to TypeGraph.ELEMENT, i.e., the type of
 * all program types.
 */
public class PTElement extends PTType {

    public PTElement(@NotNull TypeGraph g) {
        super(g);
    }

    @NotNull @Override public MTType toMath() {
        return getTypeGraph().ELEMENT;
    }

    @NotNull @Override public PTType instantiateGenerics(
            @NotNull Map<String, PTType> genericInstantiations,
            @NotNull FacilitySymbol instantiatingFacility) {
        return this;
    }
}