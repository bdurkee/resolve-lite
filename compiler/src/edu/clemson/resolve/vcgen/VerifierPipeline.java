package edu.clemson.resolve.vcgen;

import edu.clemson.resolve.compiler.AbstractCompilationPipeline;
import edu.clemson.resolve.compiler.AnnotatedModule;
import edu.clemson.resolve.RESOLVECompiler;
import edu.clemson.resolve.parser.ResolveParser;
import edu.clemson.resolve.vcgen.model.VCOutputFile;
import org.stringtemplate.v4.ST;

import java.util.List;

public class VerifierPipeline extends AbstractCompilationPipeline {

    public VerifierPipeline(RESOLVECompiler rc, List<AnnotatedModule> compilationUnits) {
        super(rc, compilationUnits);
    }

    @Override
    public void process() {
        for (AnnotatedModule unit : compilationUnits) {
            if (compiler.targetNames.contains(unit.getNameToken().getText()) && compiler.vcs) {
                if (unit.getRoot().getChild(0) instanceof ResolveParser.PrecisModuleDeclContext) continue;
                else if (unit.getRoot().getChild(0) instanceof ResolveParser.ConceptModuleDeclContext) continue;
                else if (unit.getRoot().getChild(0) instanceof ResolveParser.ConceptExtModuleDeclContext) continue;
                else if (unit.getRoot().getChild(0) instanceof ResolveParser.PrecisExtModuleDeclContext) continue;
                VCGenerator gen = new VCGenerator(compiler, unit);
                //TODO: Use log instead!
                //compiler.info("generating vcs for: " + unit.getNameToken().getText());
                VCOutputFile vcs = gen.getVCOutput();
                unit.setVCs(vcs);
                //ST x = gen.generateAssertions();
                //System.out.println(x.render());

                 List<VC> proverInput = vcs.getFinalVCs();
            }
        }
    }
}