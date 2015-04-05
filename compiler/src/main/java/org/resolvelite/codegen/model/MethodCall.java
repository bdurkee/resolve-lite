package org.resolvelite.codegen.model;

import java.util.ArrayList;
import java.util.List;

public class MethodCall extends Expr {

    public String name;
    @ModelElement public List<Expr> args = new ArrayList<>();
    @ModelElement public Qualifier q;

    public MethodCall(Qualifier qualifier, String name) {
        this.name = name;
        this.q = qualifier;
    }

    public MethodCall(VarNameRef nameRef) {
        //this(nameRef.q, "get" + nameRef.name);
        this(nameRef.q, "get" + nameRef.name);
    }
}
