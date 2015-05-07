package org.resolvelite.codegen.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementations of specifications (concepts, enhancements) require a
 * constructor (ctor) -- this class represents its definition.
 */
public class CtorDef extends OutputModelObject {
    public String name;
    public List<String> members = new ArrayList<>();
    @ModelElement public List<FacilityDef> facMems = new ArrayList<>();

    public CtorDef(String name, List<FacilityDef> facilityVars,
                   List<VariableDef> memberVars) {
        this.name = name;
        this.members.addAll(memberVars
                .stream().map(v -> v.name)
                .collect(Collectors.toList()));
        this.facMems.addAll(facilityVars);
    }
}