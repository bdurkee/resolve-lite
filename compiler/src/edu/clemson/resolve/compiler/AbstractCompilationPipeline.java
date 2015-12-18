package edu.clemson.resolve.compiler;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class AbstractCompilationPipeline {

    protected final List<AnnotatedModule> compilationUnits;
    protected final RESOLVECompiler compiler;

    public AbstractCompilationPipeline(@NotNull RESOLVECompiler compiler,
                                       @NotNull List<AnnotatedModule> compilationUnits) {
        this.compilationUnits = compilationUnits;
        this.compiler = compiler;
    }

    public abstract void process();
}