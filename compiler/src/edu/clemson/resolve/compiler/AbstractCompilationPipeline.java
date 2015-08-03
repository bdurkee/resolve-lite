package edu.clemson.resolve.compiler;

import java.util.List;

public abstract class AbstractCompilationPipeline {

    protected final List<AnnotatedTree> compilationUnits;
    protected final RESOLVECompiler compiler;

    public AbstractCompilationPipeline(RESOLVECompiler rc,
                                       List<AnnotatedTree> compilationUnits) {
        this.compilationUnits = compilationUnits;
        this.compiler = rc;
    }
    public abstract void process();
}