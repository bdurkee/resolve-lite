package edu.clemson.resolve.codegen.model;

public class OperationParameterDef extends OutputModelObject {
    @ModelElement public FunctionDef func;
    public String name;

    public OperationParameterDef(FunctionDef f) {
        this.func = f;
        this.name = f.name;
    }
}