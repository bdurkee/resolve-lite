package org.resolvelite.vcgen;

import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.resolvelite.compiler.AbstractCompilationPipeline;
import org.resolvelite.compiler.ResolveCompiler;
import org.resolvelite.compiler.tree.AnnotatedTree;
import org.stringtemplate.v4.ST;

import java.util.List;

public class VCGenPipeline extends AbstractCompilationPipeline {

    public VCGenPipeline(@NotNull ResolveCompiler rc,
            @NotNull List<AnnotatedTree> compilationUnits) {
        super(rc, compilationUnits);
    }

    @Override public void process() {
        for (AnnotatedTree unit : compilationUnits) {
            if ( compiler.targetNames.contains(unit.getName()) && compiler.vcs ) {
                compiler.info("generating vcs for: " + unit.getName());
                VCGenerator gen = new VCGenerator(compiler, unit);

                ModelBuilderProto1 x =
                        new ModelBuilderProto1(gen, compiler.symbolTable);
                ParseTreeWalker.DEFAULT.walk(x, unit.getRoot());
                //VCGenerator gen = new VCGenerator(compiler, unit);
                //ST x = gen.generateAssertions();
                //System.out.println(x.render());
            }
        }
    }
}
