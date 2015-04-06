package org.resolvelite.codegen.model;

public class MemberRef extends Expr {
    public String name;

    @ModelElement public Expr child;

    public MemberRef(String name) {
        this.name = name;
    }
}
