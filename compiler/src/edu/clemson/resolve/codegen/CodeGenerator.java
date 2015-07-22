package edu.clemson.resolve.codegen;

import edu.clemson.resolve.codegen.model.OutputModelObject;
import edu.clemson.resolve.compiler.AnnotatedTree;
import edu.clemson.resolve.compiler.RESOLVECompiler;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.misc.Nullable;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.stringtemplate.v4.ST;

import java.io.IOException;
import java.io.Writer;

public class CodeGenerator extends AbstractCodeGenerator {

    public static final String DEFAULT_LANGUAGE = "Java";

    public CodeGenerator(@NotNull RESOLVECompiler rc,
             @NotNull AnnotatedTree rootTarget) throws IllegalStateException {
        super(rc, rootTarget, DEFAULT_LANGUAGE);
    }

    private OutputModelObject buildModuleOutputModel() {
        ModelBuilder builder = new ModelBuilder(this, compiler.symbolTable);
        ParseTree root = module.getRoot();
        ParseTreeWalker.DEFAULT.walk(builder, root);
        return builder.built.get(root);
    }

    @Nullable public ST generateModule() {
        return walk(buildModuleOutputModel());
    }
}