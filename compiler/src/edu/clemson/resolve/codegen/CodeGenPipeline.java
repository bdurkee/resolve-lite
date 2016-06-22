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
        for (AnnotatedModule unit : compilationUnits) {
            JavaCodeGenerator gen = new JavaCodeGenerator(compiler, unit);
            gen.write(gen.generateModule(), gen.getFileName());
            gen.writeReferencedExternalFiles();
        }
    }
}