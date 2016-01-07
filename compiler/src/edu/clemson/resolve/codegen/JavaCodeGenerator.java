package edu.clemson.resolve.codegen;

import edu.clemson.resolve.codegen.model.OutputModelObject;
import edu.clemson.resolve.compiler.AnnotatedModule;
import edu.clemson.resolve.compiler.ErrorKind;
import edu.clemson.resolve.compiler.RESOLVECompiler;
import edu.clemson.resolve.misc.Utils;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.jetbrains.annotations.NotNull;
import org.rsrg.semantics.ModuleIdentifier;
import org.rsrg.semantics.NoSuchModuleException;
import org.stringtemplate.v4.*;

import java.io.File;
import java.io.IOException;

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