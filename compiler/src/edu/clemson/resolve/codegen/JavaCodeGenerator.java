package edu.clemson.resolve.codegen;

import edu.clemson.resolve.codegen.model.OutputModelObject;
import edu.clemson.resolve.compiler.AnnotatedModule;
import edu.clemson.resolve.RESOLVECompiler;
import edu.clemson.resolve.compiler.ErrorKind;
import edu.clemson.resolve.misc.Utils;
import edu.clemson.resolve.semantics.ModuleIdentifier;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.jetbrains.annotations.NotNull;
import org.stringtemplate.v4.AutoIndentWriter;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STWriter;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
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

    @Override
    public void write(ST code, String fileName) {
        try {
            Writer w = compiler.getOutputFileWriter(module, fileName, new Function<String, File>() {
                @Override
                public File apply(String s) {
                    Path p = module.getModuleIdentifier().getPathRelativeToRootDir();
                    String outputDir = compiler.outputDirectory;
                    if (compiler.outputDirectory.equals(".")) outputDir = "out";
                    return new File(outputDir, p.getParent().toString());
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
        for (ModuleIdentifier e : module.externalUses) {

        }
    }
}