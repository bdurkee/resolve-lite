package org.resolvelite.vcgen;

import org.antlr.v4.runtime.misc.Nullable;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.resolvelite.codegen.model.OutputModelObject;
import org.resolvelite.codegen.AbstractCodeGenerator;
import org.resolvelite.compiler.ResolveCompiler;
import org.resolvelite.compiler.tree.AnnotatedTree;
import org.stringtemplate.v4.ST;

public class VCGenerator extends AbstractCodeGenerator {

    public static final String DEFAULT_LANGUAGE = "vcs";

    public VCGenerator(ResolveCompiler rc, AnnotatedTree rootTarget)
            throws IllegalStateException {
        super(rc, rootTarget, DEFAULT_LANGUAGE);
    }

    private OutputModelObject buildVCOutputModel() {
        ModelBuilderProto o =
                new ModelBuilderProto(this, compiler.symbolTable);
        ParseTree root = module.getRoot();
        ParseTreeWalker.DEFAULT.walk(o, root);
        return o.getOutputFile();
    }

    @Nullable public ST generateAssertions() {
        return walk(buildVCOutputModel());
    }

    @Nullable public ST generateXMLAssertions() {
        throw new UnsupportedOperationException();
    }

}
