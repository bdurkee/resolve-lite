package edu.clemson.resolve.codegen.model;

import java.util.ArrayList;
import java.util.List;

public class MemberClassDef extends OutputModelObject {
    public boolean isStatic = false;
    public String name, referredToByExemplar;

    @ModelElement public List<VariableDef> fields = new ArrayList<>();

    /**
     * Holds all variable defs and stats describing some type reprs process of
     * initialization; as defined within the @{code typeImplInit} rule.
     */
    @ModelElement public List<VariableDef> initVars = new ArrayList<>();
    @ModelElement public List<Stat> initStats = new ArrayList<>();

    public MemberClassDef(String name) {
        this.name = name;
    }

}