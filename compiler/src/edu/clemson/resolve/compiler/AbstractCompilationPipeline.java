package edu.clemson.resolve.compiler;

import org.antlr.v4.runtime.misc.NotNull;

import java.util.List;

public abstract class AbstractCompilationPipeline {

    protected final List<AnnotatedTree> compilationUnits;
    protected final RESOLVECompiler compiler;

    public AbstractCompilationPipeline(@NotNull RESOLVECompiler rc,
                               @NotNull List<AnnotatedTree> compilationUnits) {
        this.compilationUnits = compilationUnits;
        this.compiler = rc;
    }
    public abstract void process();
}