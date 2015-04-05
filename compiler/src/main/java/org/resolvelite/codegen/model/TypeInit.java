package org.resolvelite.codegen.model;

public class TypeInit extends Expr {

    @ModelElement public Qualifier q;
    public String typeName, initialValue;

    public TypeInit(Qualifier q, String typeName, String initialValue) {
        this.q = q;
        this.typeName = typeName;
        this.initialValue = initialValue;
    }
}
