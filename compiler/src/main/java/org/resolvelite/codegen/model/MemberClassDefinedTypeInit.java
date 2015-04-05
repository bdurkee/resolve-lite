package org.resolvelite.codegen.model;

public class MemberClassDefinedTypeInit extends TypeInit {

    public String moduleQualifier;

    public MemberClassDefinedTypeInit(String name, String moduleQualifier) {
        super(name, "");
        this.moduleQualifier = moduleQualifier;
    }
}
