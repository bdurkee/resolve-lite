package edu.clemson.resolve.codegen.model;

public class VarNameRef extends Expr {
    public String name;
    @ModelElement public Qualifier q;

    public VarNameRef(Qualifier qualifier, String name) {
        this.name = name;
        this.q = qualifier;
    }
}