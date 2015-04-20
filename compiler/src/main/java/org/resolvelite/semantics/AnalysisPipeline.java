package org.resolvelite.semantics;

import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.resolvelite.compiler.AbstractCompilationPipeline;
import org.resolvelite.compiler.ResolveCompiler;
import org.resolvelite.compiler.tree.AnnotatedTree;
import org.resolvelite.proving.absyn.PExpBuildingListener;

import java.util.List;

public class AnalysisPipeline extends AbstractCompilationPipeline {

    public AnalysisPipeline(@NotNull ResolveCompiler rc,
            @NotNull List<AnnotatedTree> compilationUnits) {
        super(rc, compilationUnits);
    }

    @Override public void process() {
        for (AnnotatedTree unit : compilationUnits) {
            compiler.info("populating: " + unit.getName());
            ParseTreeWalker walker = new ParseTreeWalker();
            DefSymbolsAndScopes definePhase =
                    new DefSymbolsAndScopes(compiler, compiler.symbolTable,
                            unit);
            ComputeMathTypes mathTypingPhase =
                    new ComputeMathTypes(compiler, compiler.symbolTable);
            walker.walk(definePhase, unit.getRoot());
            walker.walk(mathTypingPhase, unit.getRoot());

            PrintTypes pt = new PrintTypes(mathTypingPhase.mathTypes,
                    mathTypingPhase.mathTypeValues);
            walker.walk(pt, unit.getRoot());
            int i;
            i = 0;
        }
    }
}
