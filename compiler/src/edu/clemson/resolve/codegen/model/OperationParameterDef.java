package edu.clemson.resolve.codegen.model;

import org.antlr.v4.runtime.misc.NotNull;

public class OperationParameterDef extends OutputModelObject {
    @ModelElement public FunctionDef func;
    public String name;

    public OperationParameterDef(@NotNull FunctionDef f) {
        this.func = f;
        this.name = f.name;
    }
}