package edu.clemson.resolve.codegen.model;

import org.rsrg.semantics.symbol.FacilitySymbol;

public abstract class Qualifier extends OutputModelObject {

    public static class FacilityQualifier extends Qualifier {
        public String referencedSymbolSpecName, facilityName;

        public FacilityQualifier(String referencedSymbolSpecName,
                                 String facilityName) {
            this.facilityName = facilityName;
            this.referencedSymbolSpecName = referencedSymbolSpecName;
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