package edu.clemson.resolve.codegen;

import edu.clemson.resolve.codegen.model.ModuleFile;
import edu.clemson.resolve.codegen.model.OutputModelObject;
import edu.clemson.resolve.compiler.AnnotatedModule;
import edu.clemson.resolve.RESOLVECompiler;
import edu.clemson.resolve.compiler.ErrorKind;
import edu.clemson.resolve.misc.Utils;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.jetbrains.annotations.NotNull;
import edu.clemson.resolve.semantics.ModuleIdentifier;
import org.stringtemplate.v4.AutoIndentWriter;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STWriter;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;

class JavaCodeGenerator extends AbstractCodeGenerator {

    private static final String LANGUAGE = "Java";

    JavaCodeGenerator(@NotNull RESOLVECompiler compiler, @NotNull AnnotatedModule module) {
        super(compiler, module, LANGUAGE);
    }

    @NotNull
    private OutputModelObject buildModuleOutputModel() {
        ModelBuilder builder = new ModelBuilder(this, compiler.symbolTable);
        ParseTree root = module.getRoot();
        ParseTreeWalker.DEFAULT.walk(builder, root);
        return builder.built.get(root);
    }

    ST generateModule() {
        return walk(buildModuleOutputModel());
    }

    @Override public void write(ST code, String fileName) {
        try {
            Writer w = compiler.getOutputFileWriter(module, fileName, new Function<AnnotatedModule, File>() {
                @Override
                public File apply(AnnotatedModule annotatedModule) {
                    String filePath = annotatedModule.getModulePathRelativeToProjectRoot(compiler.outputDirectory);
                    File result = new File(filePath).getParentFile(); //if we have foo/T.resolve, this gives foo/

                    //and this will stick the output directory on the front out/foo
                    return new File(compiler.outputDirectory, result.getPath());
                }
            });
            STWriter wr = new AutoIndentWriter(w);
            wr.setLineWidth(80);
            code.write(wr);
            w.close();
        } catch (IOException ioe) {
            compiler.errMgr.toolError(ErrorKind.CANNOT_WRITE_FILE,
                    ioe,
                    fileName);
        }
    }

    void writeReferencedExternalFiles() {
        //these *should* exist;
        //we've already checked in BasicSanityCheckingVisitor..
        for (ModuleIdentifier e : module.externalUses.values()) {
            String fileName = e.getNameString() + getFileExtension();
            ModuleFile moduleFile = new ModuleFile(null, fileName, compiler.genPackage);
            File externalFile = Utils.getExternalFile(compiler, e.getNameString());
            if (externalFile == null) continue;
            String contents = null;
            try {
                contents = Utils.readFile(externalFile.getPath());
            } catch (IOException ioe) {
                throw new RuntimeException(ioe.getCause());
            }
            ST result = walk(moduleFile).add("module", contents);
            write(result);
        }
    }
}