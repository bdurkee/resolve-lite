package edu.clemson.resolve.codegen.model;

import java.util.ArrayList;
import java.util.List;

public class DecoratedFacilityInstantiation extends OutputModelObject {
    public boolean isProxied;
    public String specName, specRealizName;
    @ModelElement public List<Expr> args = new ArrayList<>();
    @ModelElement public DecoratedFacilityInstantiation child;

    public DecoratedFacilityInstantiation(String specName, String specRealizName) {
        this.specName = specName;
        this.specRealizName = specRealizName;
    }
}