package org.resolvelite.codegen.model;

import org.resolvelite.semantics.symbol.FacilitySymbol;

public abstract class Qualifier extends OutputModelObject {
    //Todo: Pull this out and make it extend a generic, abstract "Qualifier"
    public static class FacilityQualifier extends Qualifier {
        public String facilitySpecName, facilityName;

        public FacilityQualifier(FacilitySymbol f) {
            this(f.getName(), f.getFacility().getSpecification().getName());
        }

        public FacilityQualifier(String facilityName, String facilitySpecName) {
            this.facilityName = facilityName;
            this.facilitySpecName = facilitySpecName;
        }
    }

    /**
     * I realize "NormalQualifier" isn't a very descriptive name. Just know
     * that it's intended to mean qualifiers without weird casts going on.
     * Think: {@code "ResolveBase.", "this.", "Test_Fac."} etc, etc.
     */
    public static class NormalQualifier extends Qualifier {
        public String name;

        public NormalQualifier(String qualifierName) {
            this.name = qualifierName;
        }
    }
}