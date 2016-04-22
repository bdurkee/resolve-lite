package edu.clemson.resolve.codegen;

import edu.clemson.resolve.codegen.model.OutputModelObject;
import edu.clemson.resolve.compiler.AnnotatedModule;
import edu.clemson.resolve.RESOLVECompiler;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.jetbrains.annotations.NotNull;
import edu.clemson.resolve.semantics.ModuleIdentifier;
import org.stringtemplate.v4.ST;

class JavaCodeGenerator extends AbstractCodeGenerator {

    private static final String LANGUAGE = "Java";

    JavaCodeGenerator(@NotNull RESOLVECompiler compiler,
                      @NotNull AnnotatedModule module) {
        super(compiler, module, LANGUAGE);
    }

    @NotNull private OutputModelObject buildModuleOutputModel() {
        ModelBuilder builder = new ModelBuilder(this, getCompiler().symbolTable);
        ParseTree root = getModule().getRoot();
        ParseTreeWalker.DEFAULT.walk(builder, root);
        return builder.built.get(root);
    }

    ST generateModule() {
        return walk(buildModuleOutputModel());
    }

    void writeReferencedExternalFiles() {
        for (ModuleIdentifier e : module.externalUses.values()) {
           ST hardcodedExternal =
                   templates.getInstanceOf(e.getNameString()).add("pkg",
                           compiler.genPackage != null ? compiler.genPackage : null);
            write(hardcodedExternal, e.getNameString()+getFileExtension());
        }
    }
}