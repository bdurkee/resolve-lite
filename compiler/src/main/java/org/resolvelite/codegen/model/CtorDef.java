package org.resolvelite.codegen.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementations of specifications (concepts & enhancements) require a
 * constructor (ctor) -- this class represents its definition.
 */
public class CtorDef extends OutputModelObject {
    public String name;
    public boolean includesBaseConcept = false;
    public List<String> members = new ArrayList<>();
    @ModelElement public List<FacilityDef> facMems = new ArrayList<>();

    public CtorDef(String name, List<FacilityDef> facilityVars,
            List<VariableDef> memberVars) {
        this.name = name;
        this.facMems.addAll(facilityVars);
    }
}
