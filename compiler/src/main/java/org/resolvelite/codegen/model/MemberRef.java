package org.resolvelite.codegen.model;

import org.resolvelite.codegen.ModelBuilder;
import org.resolvelite.semantics.programtype.PTNamed;
import org.resolvelite.semantics.programtype.PTType;

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

    public MemberRef(String name, PTType t) {
        this(name, t.toString(), ((PTNamed) t).getEnclosingModuleID());
    }
}