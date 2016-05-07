package edu.clemson.resolve.vcgen;

import edu.clemson.resolve.codegen.AbstractCodeGenerator;
import edu.clemson.resolve.compiler.AnnotatedModule;
import edu.clemson.resolve.RESOLVECompiler;
import edu.clemson.resolve.vcgen.model.VCOutputFile;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.jetbrains.annotations.NotNull;
import org.stringtemplate.v4.ST;

import java.util.List;

public class VCGenerator extends AbstractCodeGenerator {

    public static final String LANGUAGE = "vcs";

    public VCGenerator(RESOLVECompiler rc, AnnotatedModule rootTarget) {
        super(rc, rootTarget, LANGUAGE);
    }

    private VCOutputFile buildVCOutputModel() {
        ModelBuilderProto o = new ModelBuilderProto(this, compiler.symbolTable);
        ParseTree root = module.getRoot();
        ParseTreeWalker.DEFAULT.walk(o, root);
        return o.getOutputFile();
    }

    @NotNull
    public VCOutputFile getVCOutput() {
        return buildVCOutputModel();
    }

    @NotNull
    public List<VC> getProverInput() {
        return buildVCOutputModel().getFinalVCs();
    }

    public ST generateAssertions() {
        return walk(buildVCOutputModel());
    }

}