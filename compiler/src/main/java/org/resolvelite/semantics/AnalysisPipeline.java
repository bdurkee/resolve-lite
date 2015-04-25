package org.resolvelite.semantics;

import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.resolvelite.compiler.AbstractCompilationPipeline;
import org.resolvelite.compiler.ErrorKind;
import org.resolvelite.compiler.ResolveCompiler;
import org.resolvelite.compiler.tree.AnnotatedTree;
import org.resolvelite.semantics.symbol.Symbol;

import java.util.List;

public class AnalysisPipeline extends AbstractCompilationPipeline {

    public AnalysisPipeline(@NotNull ResolveCompiler rc,
            @NotNull List<AnnotatedTree> compilationUnits) {
        super(rc, compilationUnits);
    }

    @Override public void process() {
        for (AnnotatedTree unit : compilationUnits) {
            compiler.info("defining syms: " + unit.getName());
            ParseTreeWalker walker = new ParseTreeWalker();
            DefSymbolsAndScopes definePhase =
                    new DefSymbolsAndScopes(compiler, compiler.symbolTable,
                            unit);
            compiler.info("typing: " + unit.getName());
            ComputeTypes typingPhase =
                    new ComputeTypes(compiler, compiler.symbolTable, unit);
            walker.walk(definePhase, unit.getRoot());
            walker.walk(typingPhase, unit.getRoot());
            try {
                ModuleScopeBuilder curModuleScope =
                        compiler.symbolTable.getModuleScope(unit.getName());
                for (Symbol s : curModuleScope.getAllSymbols()) {
                    if (!s.containsOnlyValidTypes()) {
                        compiler.errorManager.semanticError(
                                ErrorKind.DANGLING_INVALID_TYPEREF,
                                null, s.getName());
                        unit.hasErrors = true;
                    }
                }
            } catch (NoSuchSymbolException e) {
            }
            PrintTypes pt = new PrintTypes(unit);
            walker.walk(pt, unit.getRoot());
            int i;
            i = 0;
        }
    }
}
