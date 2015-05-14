package org.resolvelite.semantics;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.resolvelite.compiler.AbstractCompilationPipeline;
import org.resolvelite.compiler.ErrorKind;
import org.resolvelite.compiler.ResolveCompiler;
import org.resolvelite.compiler.tree.AnnotatedTree;
import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.proving.absyn.PExpBuildingListener;
import org.resolvelite.semantics.symbol.Symbol;

import java.util.List;

public class AnalysisPipeline extends AbstractCompilationPipeline {

    public AnalysisPipeline(@NotNull ResolveCompiler rc,
            @NotNull List<AnnotatedTree> compilationUnits) {
        super(rc, compilationUnits);
    }

    @Override public void process() {
        for (AnnotatedTree unit : compilationUnits) {
            System.out.println("----------------------\nModule: "
                    + unit.getName() + "\n----------------------");
            ParseTreeWalker walker = new ParseTreeWalker();
            DefSymbolsAndScopes populator =
                    new DefSymbolsAndScopes(compiler, compiler.symbolTable,
                            unit);
            PExpBuildingListener<PExp> pexpAnnotator =
                    new PExpBuildingListener<>(compiler.symbolTable.mathPExps,
                            unit);
            SanityChecker sanityChecker = new SanityChecker(compiler, unit);

            walker.walk(populator, unit.getRoot());
            walker.walk(pexpAnnotator, unit.getRoot());
            walker.walk(sanityChecker, unit.getRoot());
            unit.mathPExps = compiler.symbolTable.mathPExps;
            // PrintTypes pt = new PrintTypes(unit);
            // walker.walk(pt, unit.getRoot());
        }
    }
}
