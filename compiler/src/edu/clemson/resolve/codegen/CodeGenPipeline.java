package edu.clemson.resolve.codegen;

import edu.clemson.resolve.compiler.AbstractCompilationPipeline;
import edu.clemson.resolve.compiler.AnnotatedModule;
import edu.clemson.resolve.RESOLVECompiler;
import edu.clemson.resolve.parser.ResolveParser;
import org.antlr.v4.runtime.tree.ParseTree;
import org.jetbrains.annotations.NotNull;
import org.stringtemplate.v4.ST;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class CodeGenPipeline extends AbstractCompilationPipeline {

    public CodeGenPipeline(@NotNull RESOLVECompiler compiler, @NotNull List<AnnotatedModule> compilationUnits) {
        super(compiler, compilationUnits);
    }

    @Override
    public void process() {
        if (compiler.genCode == null) return;
        File external = new File(RESOLVECompiler.getCoreLibraryDirectory() + File.separator + "external");
        for (AnnotatedModule unit : compilationUnits) {
            ParseTree t = unit.getRoot().getChild(0);
            if (t instanceof ResolveParser.PrecisModuleDeclContext ||
                    t instanceof ResolveParser.PrecisExtModuleDeclContext)
                continue;

            JavaCodeGenerator gen = new JavaCodeGenerator(compiler, unit);
            ST generatedST = gen.generateModule();
            // System.out.println("t="+generatedST.render());
            gen.write(generatedST, gen.getFileName());
            //gen.writeReferencedExternalFiles();
        }
    }
}