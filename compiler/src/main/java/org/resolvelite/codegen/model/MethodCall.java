package org.resolvelite.codegen.model;

public class MethodCall extends Expr {

    public String name, qualifier;

    public MethodCall(String qualifier, String name) {
        this.name = name;
        this.qualifier = qualifier;
    }

    public MethodCall(VarNameRef nameRef) {
        this(nameRef.qualifier, "get" + nameRef.name);
    }
}
