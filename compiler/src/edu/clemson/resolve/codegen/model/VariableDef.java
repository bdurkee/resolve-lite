package edu.clemson.resolve.codegen.model;

public class VariableDef extends OutputModelObject {
    @ModelElement
    public Expr init; //in practice, usually MethodCall and FacilityDefinedTypeInit
    public String name;

    public VariableDef(String name, Expr init) {
        this.name = name;
        this.init = init;
    }

}