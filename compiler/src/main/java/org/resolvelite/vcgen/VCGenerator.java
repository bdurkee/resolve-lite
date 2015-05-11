package org.resolvelite.vcgen;

import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.misc.Nullable;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.resolvelite.codegen.CodeGenerator;
import org.resolvelite.codegen.ModelConverter;
import org.resolvelite.codegen.model.OutputModelObject;
import org.resolvelite.compiler.ResolveCompiler;
import org.resolvelite.compiler.tree.AnnotatedTree;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;

// Todo: Make an abstract generator in 'compiler' and factor out common
// generation
// logic there maybe?
public class VCGenerator {

    public static final String TEMPLATE_ROOT =
            "org/resolvelite/templates/vcgen";

    public static final String DEFAULT_LANGUAGE = "vcs";

    protected final ResolveCompiler compiler;
    protected final AnnotatedTree module;
    private final STGroup templates;

    public final int lineWidth = 72;

    public VCGenerator(ResolveCompiler rc, AnnotatedTree rootTarget)
            throws IllegalStateException {
        this.compiler = rc;
        this.module = rootTarget;
        this.templates =
                CodeGenerator.loadTemplates(compiler, DEFAULT_LANGUAGE);
        if ( templates == null ) {
            throw new IllegalStateException();
        }
    }

    private OutputModelObject buildVCOutputModel() {
        ModelBuilder o = new ModelBuilder(this, compiler.symbolTable);
        ParseTree root = module.getRoot();
        ParseTreeWalker.DEFAULT.walk(o, root);
        return o.getOutputFile();
    }

    private ST walk(OutputModelObject outputModel) {
        ModelConverter walker = new ModelConverter(compiler, templates);
        return walker.walk(outputModel);
    }

    @Nullable public ST generateAssertions() {
        return walk(buildVCOutputModel());
    }

    @NotNull public AnnotatedTree getModule() {
        return module;
    }

    @NotNull public ResolveCompiler getCompiler() {
        return compiler;
    }

}
