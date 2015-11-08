package edu.clemson.resolve.codegen.model;

import edu.clemson.resolve.compiler.AnnotatedTree;
import edu.clemson.resolve.compiler.RESOLVECompiler;

public class ModuleFile extends OutputModelObject {
    public String RESOLVEVersion;
    public String resolveFileName;
    @ModelElement public Module module;

    public ModuleFile(AnnotatedTree e, String resolveFileName) {
        this.resolveFileName = resolveFileName;
        this.RESOLVEVersion = RESOLVECompiler.VERSION;
    }
}
