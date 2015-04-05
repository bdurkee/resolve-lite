package org.resolvelite.codegen.model;

import java.util.ArrayList;
import java.util.List;

public class ConceptImpl extends Module {
    public String concept;
    @ModelElement public List<FacilityDecl> facilityVars = new ArrayList<>();
    @ModelElement public CtorDef ctor;

    public ConceptImpl(String name, String concept, ModuleFile file) {
        super(name, file);
        this.concept = concept;
    }

    public void addCtor() {
        this.ctor = new CtorDef(this.name, facilityVars, memberVars);
    }
}
