package org.resolvelite.codegen.model;

public class VarNameRef extends Expr {
    public String qualifier, name;

    public VarNameRef(String qualifier, String name) {
        this.name = name;
        this.qualifier = qualifier;
    }
}
