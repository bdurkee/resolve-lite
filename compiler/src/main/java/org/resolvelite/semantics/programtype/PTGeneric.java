package org.resolvelite.semantics.programtype;

import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.proving.absyn.PSymbol;
import org.resolvelite.proving.absyn.PSymbol.PSymbolBuilder;
import org.resolvelite.semantics.MTNamed;
import org.resolvelite.semantics.MTType;
import org.resolvelite.typereasoning.TypeGraph;

public class PTGeneric extends PTType {

    private final String name;

    public PTGeneric(TypeGraph g, String name) {
        super(g);
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override public MTType toMath() {
        return new MTNamed(getTypeGraph(), name);
    }

    /*@Override
    public PTType instantiateGenerics(
            Map<String, PTType> genericInstantiations,
            FacilityEntry instantiatingFacility) {

        PTType result = this;

        if (genericInstantiations.containsKey(myName)) {
            result = genericInstantiations.get(myName);
        }

        return result;
    }*/

    @Override public boolean equals(Object o) {
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