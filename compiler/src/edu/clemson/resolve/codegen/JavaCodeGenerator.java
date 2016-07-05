package edu.clemson.resolve.codegen;

import edu.clemson.resolve.codegen.Model.OutputModelObject;
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
import java.nio.file.Files;
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

    @Override
    protected void write(@NotNull ModuleIdentifier moduleIdentifier,
                         @NotNull ST code,
                         @NotNull String outputFileName) {
        try {
            Writer w = compiler.getOutputFileWriter(moduleIdentifier,
                    outputFileName, new Function<String, File>() {
                @Override
                public File apply(String s) {
                    Path p = moduleIdentifier.getPathRelativeToRootDir();
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
                    outputFileName);
        }
    }

    public void writeAllExternallyReferencedFiles() {
        for (ModuleIdentifier e : module.externalUses) {
            try {
                ST code = templates.getInstanceOf("externalClassCode").add("code",
                        new String(Files.readAllBytes(Paths.get(e.getFile().getPath()))));
                write(e, code, e.getNameString() + getFileExtension());
            } catch (IOException e1) {
                e1.printStackTrace();   //failed to write external file.
            }
        }
    }
}