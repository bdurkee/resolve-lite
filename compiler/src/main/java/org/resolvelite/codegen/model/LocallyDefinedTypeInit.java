package org.resolvelite.codegen.model;

public class LocallyDefinedTypeInit extends TypeInit {

    public String moduleQualifier;

    public LocallyDefinedTypeInit(String name, String moduleQualifier) {
        super(name, "");
        this.moduleQualifier = moduleQualifier;
    }
}
