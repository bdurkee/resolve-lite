package org.resolvelite.codegen.model;

import java.util.ArrayList;
import java.util.List;

public class ConceptImplModule extends Module implements SpecImplModule {
    public String concept;
    @ModelElement public List<FacilityDef> facilityVars = new ArrayList<>();
    @ModelElement public CtorDef ctor;

    public ConceptImplModule(String name, String concept, ModuleFile file) {
        super(name, file);
        this.concept = concept;
    }

    public void addCtor() {
        this.ctor = new CtorDef(this.name, facilityVars, memberVars);
    }
}
