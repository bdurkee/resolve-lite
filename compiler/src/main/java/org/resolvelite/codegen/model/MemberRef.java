package org.resolvelite.codegen.model;

import org.resolvelite.semantics.Type;

public class MemberRef extends Expr {
    public String name;

    @ModelElement public Expr child;
    public String typeName, typeQualifier;
    public boolean isBaseRef = false;
    public boolean isLastRef = false;

    public MemberRef(String name, String typeName, String typeQualifier) {
        this.name = name;
        this.typeName = typeName;
        this.typeQualifier = typeQualifier;
        this.isBaseRef = isBaseRef;
    }

    public MemberRef(String name, Type t) {
        this(name, t.getName(), t.getRootModuleID());
    }
}
