package edu.clemson.resolve.semantics;

import edu.clemson.resolve.compiler.AbstractCompilationPipeline;
import edu.clemson.resolve.compiler.AnnotatedTree;
import edu.clemson.resolve.compiler.ResolveCompiler;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

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
         /*   DefSymbolsAndScopes defSymsAndScopes =
                    new DefSymbolsAndScopes(compiler, compiler.symbolTable,
                            unit);
            PExpBuildingListener<PExp> pexpAnnotator =
                    new PExpBuildingListener<>(compiler.symbolTable.mathPExps,
                            unit);
            SanityChecker sanityChecker = new SanityChecker(compiler, unit);

            walker.walk(defSymsAndScopes, unit.getRoot());
            walker.walk(sanityChecker, unit.getRoot());
            if ( compiler.errorManager.getErrorCount() > 0 ) return;
            walker.walk(pexpAnnotator, unit.getRoot());
            unit.mathPExps = compiler.symbolTable.mathPExps;*/
            // PrintTypes pt = new PrintTypes(unit);
            // walker.walk(pt, unit.getRoot());
        }
    }
}