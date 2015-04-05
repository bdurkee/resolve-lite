package org.resolvelite.codegen.model;

import java.util.ArrayList;
import java.util.List;

public class CtorDef extends OutputModelObject {

    public String name;
    public boolean includesBaseConcept = false;
    public List<String> members = new ArrayList<>();
    @ModelElement public List<FacilityDecl> facMems = new ArrayList<>();

    public CtorDef(String name, List<FacilityDecl> facilityVars,
            List<VariableDecl> memberVars) {
        this.name = name;
        this.facMems.addAll(facilityVars);
    }
}
