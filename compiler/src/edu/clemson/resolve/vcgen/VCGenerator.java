package edu.clemson.resolve.vcgen;

import edu.clemson.resolve.codegen.AbstractCodeGenerator;
import edu.clemson.resolve.codegen.model.OutputModelObject;
import edu.clemson.resolve.compiler.AnnotatedTree;
import edu.clemson.resolve.compiler.RESOLVECompiler;
import org.antlr.v4.runtime.misc.Nullable;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.stringtemplate.v4.ST;

public class VCGenerator extends AbstractCodeGenerator {

    public static final String DEFAULT_LANGUAGE = "vcs";

    public VCGenerator(RESOLVECompiler rc, AnnotatedTree rootTarget)
            throws IllegalStateException {
        super(rc, rootTarget, DEFAULT_LANGUAGE);
    }

    private OutputModelObject buildVCOutputModel() {
        ModelBuilderProto o = new ModelBuilderProto(this, compiler.symbolTable);
        ParseTree root = module.getRoot();
        ParseTreeWalker.DEFAULT.walk(o, root);
        return o.getOutputFile();
    }

    @Nullable
    public ST generateAssertions() {
        return walk(buildVCOutputModel());
    }

    @Nullable public ST generateXMLAssertions() {
        throw new UnsupportedOperationException();
    }

}