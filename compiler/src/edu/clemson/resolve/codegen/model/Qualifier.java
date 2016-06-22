package edu.clemson.resolve.codegen.model;

public abstract class Qualifier extends OutputModelObject {

    public static class FacilityQualifier extends Qualifier {
        public String fullyQualifiedSymbolSpecName, facilityName;

        public FacilityQualifier(String fullyQualifiedSymbolSpecName,
                                 String facilityName) {
            this.facilityName = facilityName;
            this.fullyQualifiedSymbolSpecName = fullyQualifiedSymbolSpecName;
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