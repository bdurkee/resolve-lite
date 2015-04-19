package org.resolvelite.semantics;

public class SpecImplementationPairing {

    private final ModuleParameterization spec, implementation;

    public SpecImplementationPairing(ModuleParameterization spec) {
        this(spec, null);
    }

    public SpecImplementationPairing(ModuleParameterization spec,
            ModuleParameterization impl) {
        if ( spec == null ) throw new IllegalArgumentException("null spec");
        this.spec = spec;
        this.implementation = impl;
    }

    public ModuleParameterization getSpecification() {
        return spec;
    }

    public ModuleParameterization getImplementation() {
        //if (myRealization == null) {
        //    throw new NoneProvidedException();
        // }
        return implementation;
    }
}
