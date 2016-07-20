package edu.clemson.resolve.vcgen;

import edu.clemson.resolve.compiler.AbstractCompilationPipeline;
import edu.clemson.resolve.compiler.AnnotatedModule;
import edu.clemson.resolve.RESOLVECompiler;
import edu.clemson.resolve.parser.ResolveParser;
import edu.clemson.resolve.proving.CongruenceClassProver;
import edu.clemson.resolve.vcgen.model.VCOutputFile;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.stringtemplate.v4.ST;

import java.util.ArrayList;
import java.util.List;

public class VerifierPipeline extends AbstractCompilationPipeline {

    public VerifierPipeline(RESOLVECompiler rc, List<AnnotatedModule> compilationUnits) {
        super(rc, compilationUnits);
    }

    @Override
    public void process() {
        for (AnnotatedModule unit : compilationUnits) {
            //prove implies genn'ing vcs...
            if (compiler.targetNames.contains(unit.getNameToken().getText()) && (compiler.vcs || compiler.prove)) {
                if (unit.getRoot().getChild(0) instanceof ResolveParser.PrecisModuleDeclContext) continue;
                else if (unit.getRoot().getChild(0) instanceof ResolveParser.ConceptModuleDeclContext) continue;
                else if (unit.getRoot().getChild(0) instanceof ResolveParser.ConceptExtModuleDeclContext) continue;
                else if (unit.getRoot().getChild(0) instanceof ResolveParser.PrecisExtModuleDeclContext) continue;
                VCGenerator gen = new VCGenerator(compiler, unit);
                //TODO: Use log instead!
                //compiler.info("generating vcs for: " + unit.getNameToken().getText());
                VCOutputFile vcs = gen.getVCOutput();

                //give the vc output info into the AnnotatedModule
                unit.setVCs(vcs);
                ST x = gen.generateAssertions();
                System.out.println(x.render());
                List<VC> proverInput = new ArrayList<>(); //vcs.getFinalVCs();
                proverInput.add(vcs.getFinalVCs().get(1));
                VCClassftnPrintingListener p = new VCClassftnPrintingListener(compiler);
                for (VC vc : proverInput) {
                    vc.getAntecedent().accept(p);
                    vc.getConsequent().accept(p);
                }
                if (compiler.prove) {
                    CongruenceClassProver prover = new CongruenceClassProver(compiler, unit,
                            compiler.symbolTable.getTypeGraph(), proverInput);
                }

                int i;
                i=0;
            }
        }
    }
}