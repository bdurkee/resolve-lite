package org.resolvelite.codegen.model;

public class MemberClassDef extends OutputModelObject {

    public boolean isStatic = false;
    public String name;

    public MemberClassDef(String name) {
        this.name = name;
    }
}
