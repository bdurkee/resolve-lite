package org.resolvelite.codegen.model;

import java.util.ArrayList;
import java.util.List;

public class MemberClassDef extends OutputModelObject {

    public boolean isStatic = false;
    public String name;
    @ModelElement public List<VariableDecl> fields = new ArrayList<>();

    public MemberClassDef(String name) {
        this.name = name;
    }
}
