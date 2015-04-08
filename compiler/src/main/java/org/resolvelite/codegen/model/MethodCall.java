package org.resolvelite.codegen.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MethodCall extends Expr {
    public String name;
    @ModelElement public List<Expr> args = new ArrayList<>();
    @ModelElement public Qualifier q;

    public MethodCall(Qualifier qualifier, String name, List<Expr> args) {
        this.name = name;
        this.q = qualifier;
        this.args.addAll(args);
    }

    /**
     * Two special constructors used to create create getter calls for variables
     * referencing things like module level generics and formal params
     */
    public MethodCall(VarNameRef nameRef) {
        this(nameRef.q, "get" + nameRef.name, Collections.emptyList());
    }

    public MethodCall(TypeInit genericTypeInit) {
        this(genericTypeInit.q, "get" + genericTypeInit.typeName, Collections
                .emptyList());
    }
}
