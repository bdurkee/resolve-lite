package org.rsrg.semantics.programtype;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rsrg.semantics.MTNamed;
import org.rsrg.semantics.MTType;
import org.rsrg.semantics.symbol.FacilitySymbol;
import org.rsrg.semantics.TypeGraph;

import java.util.Map;

public class PTGeneric extends PTType {

    @NotNull private final String name;

    public PTGeneric(@NotNull TypeGraph g, @NotNull String name) {
        super(g);
        this.name = name;
    }

    @NotNull public String getName() {
        return name;
    }

    @NotNull @Override public MTType toMath() {
        return new MTNamed(getTypeGraph(), name);
    }

    @NotNull @Override public PTType instantiateGenerics(
            @NotNull Map<String, PTType> genericInstantiations,
            @NotNull FacilitySymbol instantiatingFacility) {
        PTType result = this;
        if ( genericInstantiations.containsKey(name) ) {
            result = genericInstantiations.get(name);
        }
        return result;
    }

    @Override public boolean equals(@Nullable Object o) {
        boolean result = (o instanceof PTGeneric);

        if ( result ) {
            PTGeneric oAsPTGeneric = (PTGeneric) o;
            result = name.equals(oAsPTGeneric.getName());
        }
        return result;
    }

    @Override public String toString() {
        return name;
    }

}