package edu.clemson.resolve.codegen.model;

import org.rsrg.semantics.programtype.PTNamed;
import org.rsrg.semantics.programtype.PTType;

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
        this(name, ((PTNamed) t).getName(), ((PTNamed) t)
                .getModuleIdentifier());
    }
}