package edu.clemson.resolve.codegen;

import edu.clemson.resolve.compiler.AbstractCompilationPipeline;
import edu.clemson.resolve.compiler.AnnotatedModule;
import edu.clemson.resolve.RESOLVECompiler;
import org.jetbrains.annotations.NotNull;

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
            gen.write(gen.generateModule(), gen.getOutputFileName());
            gen.writeAllExternallyReferencedFiles();
        }
    }
}