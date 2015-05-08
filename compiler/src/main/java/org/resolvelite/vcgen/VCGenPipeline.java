package org.resolvelite.vcgen;

import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.resolvelite.codegen.CodeGenerator;
import org.resolvelite.compiler.AbstractCompilationPipeline;
import org.resolvelite.compiler.ResolveCompiler;
import org.resolvelite.compiler.tree.AnnotatedTree;
import org.resolvelite.semantics.NoSuchSymbolException;

import java.util.List;

public class VCGenPipeline extends AbstractCompilationPipeline {

    public VCGenPipeline(@NotNull ResolveCompiler rc,
            @NotNull List<AnnotatedTree> compilationUnits) {
        super(rc, compilationUnits);
    }

    @Override public void process() {
        for (AnnotatedTree unit : compilationUnits) {
            VCGenerator gen =
                    new VCGenerator(compiler, compiler.symbolTable, unit);
            if ( compiler.vcs ) {
                ParseTreeWalker.DEFAULT.walk(gen, unit.getRoot());
            }
        }

    }
}
