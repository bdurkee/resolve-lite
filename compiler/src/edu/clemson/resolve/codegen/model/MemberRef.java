package edu.clemson.resolve.codegen.model;

import edu.clemson.resolve.semantics.programtype.ProgNamedType;
import edu.clemson.resolve.semantics.programtype.ProgType;

public class MemberRef extends Expr {
    public String name;

    @ModelElement
    public Expr child;
    public String typeName, typeQualifier;
    public boolean isBaseRef = false;
    public boolean isLastRef = false;

    public MemberRef(String name, String typeName, String typeQualifier) {
        this.name = name;
        this.typeName = typeName;
        this.typeQualifier = typeQualifier;
        this.isBaseRef = isBaseRef;
    }

    public MemberRef(String name, ProgType t) {
        this(name, ((ProgNamedType) t).getName(), ((ProgNamedType) t)
                .getModuleIdentifier().getNameToken().getText());
    }
}