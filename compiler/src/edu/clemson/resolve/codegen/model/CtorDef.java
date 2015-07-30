package edu.clemson.resolve.codegen.model;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/** Implementations of specifications (concepts, enhancements) require
 *  constructor(s) (ctor) -- this class represents its definition.
 */
public class CtorDef extends OutputModelObject {

    public String name, delegateInterface;
    public List<String> members = new ArrayList<>();
    @ModelElement public List<FacilityDef> facMems = new ArrayList<>();

    public CtorDef(String name, String delegateInterface,
                   List<FacilityDef> facilityVars,
                   List<VariableDef> memberVars) {
        this.name = name;
        this.delegateInterface = delegateInterface;
        this.members.addAll(memberVars
                .stream().map(v -> v.name)
                .collect(Collectors.toList()));
        this.facMems.addAll(facilityVars);
    }
}