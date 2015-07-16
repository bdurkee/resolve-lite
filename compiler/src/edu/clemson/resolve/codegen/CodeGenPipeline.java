package edu.clemson.resolve.codegen;

import edu.clemson.resolve.compiler.AbstractCompilationPipeline;
import edu.clemson.resolve.compiler.AnnotatedTree;
import edu.clemson.resolve.compiler.RESOLVECompiler;
import edu.clemson.resolve.parser.Resolve;
import org.antlr.v4.runtime.misc.NotNull;
import org.stringtemplate.v4.ST;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class CodeGenPipeline extends AbstractCompilationPipeline {

    public CodeGenPipeline(@NotNull RESOLVECompiler rc,
                           @NotNull List<AnnotatedTree> compilationUnits) {
        super(rc, compilationUnits);
    }

    @Override public void process() {
        if ( compiler.genCode == null ) return;
        File outputDir = new File(compiler.outputDirectory);

        for (AnnotatedTree unit : compilationUnits) {
            try {
                if ( unit.getRoot().getChild(0) instanceof Resolve.PrecisModuleContext )
                    continue;
                CodeGenerator gen = new CodeGenerator(compiler, unit);
                compiler.info("generating code: " + unit.getName());
                if ( compiler.genCode.equals("Java") ) {
                    ST x = gen.generateModule();
                    //System.out.println(x.render());
                    gen.writeFile(x);
                }
                /*for (String external : unit.imports
                        .getImportsOfType(ImportCollection.ImportType.EXTERNAL)) {
                    FileLocator l =
                            new FileLocator(external,
                                    ResolveCompiler.NON_NATIVE_EXT);
                    Files.walkFileTree(
                            new File(compiler.libDirectory).toPath(), l);
                    File srcFile = l.getFile();
                    Path srcPath = srcFile.toPath();
                    Path destPath =
                            new File(outputDir.getName() + "/"
                                    + srcFile.getName()).toPath();
                    Files.copy(srcPath, destPath,
                            StandardCopyOption.REPLACE_EXISTING);
                }*/
            }
            catch (IllegalStateException ise) {
                return; //if the templates were unable to be loaded, etc.
            }
            //catch (IOException ioe) {
            //    throw new RuntimeException(ioe.getCause().getMessage());
            //}
        }
    }

}