package edu.clemson.resolve.vcgen;

import edu.clemson.resolve.compiler.AbstractCompilationPipeline;
import edu.clemson.resolve.compiler.AnnotatedTree;
import edu.clemson.resolve.compiler.RESOLVECompiler;
import org.antlr.v4.runtime.misc.NotNull;
import org.stringtemplate.v4.ST;

import java.util.List;

public class VCGenPipeline extends AbstractCompilationPipeline {

    public VCGenPipeline(@NotNull RESOLVECompiler rc,
                         @NotNull List<AnnotatedTree> compilationUnits) {
        super(rc, compilationUnits);
    }

    @Override public void process() {
        for (AnnotatedTree unit : compilationUnits) {
            if ( compiler.targetNames.contains(unit.getName()) && compiler.vcs ) {
                    VCGenerator gen = new VCGenerator(compiler, unit);
                    compiler.info("generating vcs for: " + unit.getName());

                    ST x = gen.generateAssertions();
                    System.out.println(x.render());


            }
        }
    }
}