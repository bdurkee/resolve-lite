package org.resolvelite.codegen.model;

public class ConceptImpl extends Module {
    public String concept;

    public ConceptImpl(String name, String concept, ModuleFile file) {
        super(name, file);
        this.concept = concept;
    }
}
