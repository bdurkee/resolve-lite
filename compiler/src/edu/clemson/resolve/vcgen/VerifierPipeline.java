package edu.clemson.resolve.vcgen;

import edu.clemson.resolve.compiler.AbstractCompilationPipeline;
import edu.clemson.resolve.compiler.AnnotatedModule;
import edu.clemson.resolve.compiler.RESOLVECompiler;
import edu.clemson.resolve.parser.ResolveParser;
import org.stringtemplate.v4.ST;

import java.util.List;

public class VerifierPipeline extends AbstractCompilationPipeline {

    public VerifierPipeline(RESOLVECompiler rc,
                            List<AnnotatedModule> compilationUnits) {
        super(rc, compilationUnits);
    }

    @Override public void process() {
        for (AnnotatedModule unit : compilationUnits) {
            if ( compiler.targetNames.contains(unit.getName()) && compiler.vcs ) {
                if (unit.getRoot().getChild(0) instanceof
                        ResolveParser.PrecisModuleDeclContext) continue;
               /* else if (unit.getRoot().getChild(0) instanceof
                        ResolveParser.ConceptModuleContext) continue;
                else if (unit.getRoot().getChild(0) instanceof
                        ResolveParser.ExtensionModuleContext) continue;*/
                VCGenerator gen = new VCGenerator(compiler, unit);
                compiler.info("generating vcs for: " + unit.getName());
                ST x = gen.generateAssertions();
                //List<VC> vcs = gen.getProverInput();
                System.out.println(x.render());
                //TODO: Hook up conguence class prover.
              //  ModelBuilderProto2 vv = new ModelBuilderProto2();
              //  ParseTreeWalker.DEFAULT.walk(vv, unit.getRoot());
            }
        }
    }
}