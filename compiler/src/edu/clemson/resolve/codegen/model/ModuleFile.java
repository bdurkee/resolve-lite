package edu.clemson.resolve.codegen.model;

import edu.clemson.resolve.compiler.AnnotatedModule;
import edu.clemson.resolve.RESOLVECompiler;

import java.util.ArrayList;
import java.util.List;

public class ModuleFile extends OutputModelObject {
    public String RESOLVEVersion;
    public String resolveFileName;

    @ModelElement
    public Module module;
    public String genPackage; // from -package cmd-line
    public List<String> imports = new ArrayList<>();

    public ModuleFile(AnnotatedModule e, String resolveFileName, String pkg, List<String> imports) {
        this.resolveFileName = resolveFileName;
        this.RESOLVEVersion = RESOLVECompiler.VERSION;
        this.genPackage = pkg;
        this.imports.addAll(imports);
    }
}
