package edu.clemson.resolve.analysis;

import edu.clemson.resolve.compiler.AbstractCompilationPipeline;
import edu.clemson.resolve.compiler.AnnotatedModule;
import edu.clemson.resolve.RESOLVECompiler;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PExpBuildingListener;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AnalysisPipeline extends AbstractCompilationPipeline {

    public AnalysisPipeline(@NotNull RESOLVECompiler rc,
                            @NotNull List<AnnotatedModule> compilationUnits) {
        super(rc, compilationUnits);
    }

    @Override
    public void process() {
        //if (compiler.errMgr.getErrorCount() > 0) return;
        for (AnnotatedModule unit : compilationUnits) {
            compiler.log("AnalysisPipeline",
                    "populating: " + unit.getNameToken().getText());
            ParseTreeWalker walker = new ParseTreeWalker();
            
            BasicSanityCheckingVisitor initialSanityChecks =
                    new BasicSanityCheckingVisitor(compiler, unit);
            initialSanityChecks.visit(unit.getRoot());
            PopulatingVisitor defSymsAndScopes =
                    new PopulatingVisitor(compiler, compiler.symbolTable,
                            unit);
            defSymsAndScopes.visit(unit.getRoot());
            PExpBuildingListener<PExp> pexpAnnotator =
                    new PExpBuildingListener<>(
                            defSymsAndScopes.getTypeGraph(), unit);
            SanityCheckingListener sanityChecker =
                    new SanityCheckingListener(compiler, unit);
            walker.walk(pexpAnnotator, unit.getRoot());
            walker.walk(sanityChecker, unit.getRoot());
            if (compiler.errMgr.getErrorCount() > 0) return;

            MathClssftnLogger pl =
                    new MathClssftnLogger(compiler, unit.mathClssftns);
            ParseTreeWalker.DEFAULT.walk(pl, unit.getRoot());

        }
    }
}