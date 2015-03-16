package resolvelite.compiler;

import org.antlr.v4.runtime.misc.NotNull;
import resolvelite.compiler.tree.ResolveAnnotatedParseTree;

import java.util.List;

public abstract class AbstractCompilationPipeline {

    @NotNull protected final List<ResolveAnnotatedParseTree> myCompilationUnits;
    @NotNull protected final ResolveCompiler myCompiler;

    public AbstractCompilationPipeline(@NotNull ResolveCompiler rc,
            @NotNull List<ResolveAnnotatedParseTree> compilationUnits) {
        myCompilationUnits = compilationUnits;
        myCompiler = rc;
    }

    public abstract void process();
}
