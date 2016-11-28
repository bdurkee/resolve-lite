package edu.clemson.resolve.vcgen;

import edu.clemson.resolve.compiler.AbstractCompilationPipeline;
import edu.clemson.resolve.compiler.AnnotatedModule;
import edu.clemson.resolve.RESOLVECompiler;
import edu.clemson.resolve.parser.ResolveParser;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

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
                VCGen gen = new VCGen(compiler, unit);
                ParseTreeWalker.DEFAULT.walk(gen, unit.getRoot());
                VCOutputFile x = gen.getOutputFile();
                //System.out.println(x);
                unit.setVCs(x);
                int i;
                i=0;
                //List<VC> proverInput = vco.getFinalVCs();
                //VCClassftnPrintingListener p = new VCClassftnPrintingListener(compiler);
                /*for (VC vc : proverInput) {
                    vc.getAntecedent().accept(p);
                    vc.getConsequent().accept(p);
                }*/
                //List<VC> pvcs = new ArrayList<>();
                //pvcs.add(vco.getFinalVCs().get(0));
                /*if (compiler.prove) {
                    CongruenceClassProver prover = new CongruenceClassProver(compiler, unit,
                            compiler.symbolTable.getTypeGraph(), vco.getFinalVCs());
                    try {
                        prover.start();
                    }
                    catch (IOException ioe) {
                        throw new RuntimeException(ioe);
                    }
                }*/
            }
        }
    }
}