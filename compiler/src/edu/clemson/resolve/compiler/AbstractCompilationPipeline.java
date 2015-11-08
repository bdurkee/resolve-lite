package edu.clemson.resolve.compiler;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class AbstractCompilationPipeline {

    @NotNull protected final List<AnnotatedTree> compilationUnits;
    @NotNull protected final RESOLVECompiler compiler;

    public AbstractCompilationPipeline(@NotNull RESOLVECompiler compiler,
                                       @NotNull List<AnnotatedTree> compilationUnits) {
        this.compilationUnits = compilationUnits;
        this.compiler = compiler;
    }

    public abstract void process();
}