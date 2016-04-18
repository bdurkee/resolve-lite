package edu.clemson.resolve.codegen.model;

import edu.clemson.resolve.compiler.AnnotatedModule;
import edu.clemson.resolve.RESOLVECompiler;

public class ModuleFile extends OutputModelObject {
    public String RESOLVEVersion;
    public String resolveFileName;
    @ModelElement public Module module;
    public String genPackage; // from -package cmd-line

    public ModuleFile(AnnotatedModule e, String resolveFileName, String pkg) {
        this.resolveFileName = resolveFileName;
        this.RESOLVEVersion = RESOLVECompiler.VERSION;
        this.genPackage = pkg;
    }
}
