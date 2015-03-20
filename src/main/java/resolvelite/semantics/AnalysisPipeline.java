package resolvelite.semantics;

import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import resolvelite.compiler.AbstractCompilationPipeline;
import resolvelite.compiler.ResolveCompiler;
import resolvelite.compiler.tree.ResolveAnnotatedParseTree.TreeAnnotatingBuilder;

import java.util.List;

public class AnalysisPipeline extends AbstractCompilationPipeline {

    public AnalysisPipeline(@NotNull ResolveCompiler rc,
            @NotNull List<TreeAnnotatingBuilder> compilationUnits) {
        super(rc, compilationUnits);
    }

    @Override
    public void process() {
        for (TreeAnnotatingBuilder unit : compilationUnits) {
            compiler.info("populating: " + unit.name.getText());
            ParseTreeWalker walker = new ParseTreeWalker();
            DefSymbolsAndScopes definePhase = new DefSymbolsAndScopes();
            walker.walk(definePhase, unit.root);
        }
    }
}
