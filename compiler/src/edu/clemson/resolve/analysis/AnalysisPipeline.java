package edu.clemson.resolve.analysis;

import edu.clemson.resolve.compiler.AbstractCompilationPipeline;
import edu.clemson.resolve.compiler.AnnotatedTree;
import edu.clemson.resolve.compiler.RESOLVECompiler;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PExpBuildingListener;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.util.List;

public class AnalysisPipeline extends AbstractCompilationPipeline {

    public AnalysisPipeline(@NotNull RESOLVECompiler rc,
                        @NotNull List<AnnotatedTree> compilationUnits) {
        super(rc, compilationUnits);
    }

    @Override public void process() {
        if (compiler.errMgr.getErrorCount() > 0) return;
        for (AnnotatedTree unit : compilationUnits) {
            compiler.info("populating: " + unit.getName());
            ParseTreeWalker walker = new ParseTreeWalker();
            System.out.println("----------------------\nModule: "
                    + unit.getName() + "\n----------------------");
            PopulatingVisitor defSymsAndScopes =
                    new PopulatingVisitor(compiler, compiler.symbolTable,
                            unit);
            defSymsAndScopes.visit(unit.getRoot());
            PExpBuildingListener<PExp> pexpAnnotator =
                    new PExpBuildingListener<>(compiler.symbolTable.mathPExps,
                            unit);
            //SanityChecker sanityChecker = new SanityChecker(compiler, unit);

            //walker.walk(sanityChecker, unit.getRoot());
            if ( compiler.errMgr.getErrorCount() > 0 ) return;
            walker.walk(pexpAnnotator, unit.getRoot());
            unit.mathPExps = compiler.symbolTable.mathPExps;
        }
    }
}