package edu.clemson.resolve.codegen.model;

import edu.clemson.resolve.semantics.symbol.Symbol;

import java.util.List;

public class FacilityDef extends OutputModelObject {
    public boolean isStatic = false;
    public String name, concept;
    @ModelElement public DecoratedFacilityInstantiation root;

    public FacilityDef(String name, String concept) {
        this.name = name;
        this.concept = concept;
    }

    public void addGettersForGenericsAndNamedVariableArguments(
            List<? extends Symbol> symbols) {}
}