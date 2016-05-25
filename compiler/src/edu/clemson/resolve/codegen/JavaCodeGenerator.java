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
            Writer w = compiler.getOutputFileWriter(module, fileName,
                    new javaOutputFun(compiler.libDirectory, compiler.outputDirectory));
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

    public static class javaOutputFun implements Function<AnnotatedModule, File> {

        @NotNull private final String libDir, specifiedOutputDir;

        public javaOutputFun(@NotNull String libDir, @NotNull String specifiedOutputDir) {
            this.libDir = libDir;
            this.specifiedOutputDir = specifiedOutputDir;
        }

        @Override
        public File apply(AnnotatedModule annotatedModule) {

            String resolveRoot = RESOLVECompiler.getCoreLibraryDirectory() + File.separator + "src";
            String resolvePath = RESOLVECompiler.getLibrariesPathDirectory() + File.separator + "src";

            Path filePathAbsolute = Paths.get(libDir);
            Path projectPathAbsolute = null;
            File result = null;
            //is the current file on $RESOLVEPATH?
            if (filePathAbsolute.startsWith(resolvePath)) {
                projectPathAbsolute = Paths.get(new File(resolvePath).getAbsolutePath());
                Path pathRelative = projectPathAbsolute.relativize(filePathAbsolute);

                //concatenate -o dir + trimmed subfolders
                result = new File(specifiedOutputDir, pathRelative.toFile().getPath());
            }
            else if (filePathAbsolute.startsWith(resolveRoot)) {
                projectPathAbsolute = Paths.get(new File(resolveRoot).getAbsolutePath());
                Path pathRelative = projectPathAbsolute.relativize(filePathAbsolute);
                result = new File(specifiedOutputDir, pathRelative.toFile().getPath());
            }
            else {
                //just use the lib directory if the user has a non-conformal project..
                result = new File(specifiedOutputDir, filePathAbsolute.toFile().getPath());
            }
            return result;
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