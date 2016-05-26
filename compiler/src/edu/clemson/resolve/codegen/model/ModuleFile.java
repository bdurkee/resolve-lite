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
    @ModelElement public List<ModuleImport> imports = new ArrayList<>();

    public ModuleFile(AnnotatedModule e, String resolveFileName, String pkg, List<ModuleImport> imports) {
        this.resolveFileName = resolveFileName;
        this.RESOLVEVersion = RESOLVECompiler.VERSION;
        this.genPackage = pkg;
        this.imports.addAll(imports);
    }

    public static class ModuleImport extends OutputModelObject {
        /**
         * filepath represents an import. This list segments each directory. So if we use T, and T is located
         * in user/local/temp/T.resolve, then this will store [user, local, temp] (the last name is stored in
         * {@code name}
         */
        public List<String> components = new ArrayList<>();

        public String importName;

        public ModuleImport(List<String> components, String importName) {
            this.importName = importName;
            this.components.addAll(components);
        }
    }
}
