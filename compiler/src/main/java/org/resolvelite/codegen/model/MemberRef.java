package org.resolvelite.codegen.model;

import org.resolvelite.semantics.Type;

public class MemberRef extends Expr {
    public String name;

    @ModelElement public Expr child;
    public String typeName, typeQualifier;
    public boolean isOutermost = false;
    public MemberRef(String name, String typeName, String typeQualifier) {
        this.name = name;
        this.typeName = typeName;
        this.typeQualifier = typeQualifier;
    }

    public MemberRef(String name, Type t) {
        this(name, t.getName(), t.getRootModuleID());
    }
}
