package org.resolvelite.codegen.model;

public abstract class TypeInit extends Expr {

    public String typeName, initialValue;

    public TypeInit(String typeName, String initialValue) {
        this.typeName = typeName;
        this.initialValue = initialValue;
    }
}
