package edu.clemson.resolve.codegen;

import edu.clemson.resolve.codegen.model.OutputModelObject;
import edu.clemson.resolve.compiler.AnnotatedModule;
import edu.clemson.resolve.compiler.RESOLVECompiler;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.jetbrains.annotations.NotNull;
import org.rsrg.semantics.NoSuchModuleException;
import org.stringtemplate.v4.*;

public class JavaCodeGenerator extends AbstractCodeGenerator {

    public static final String LANGUAGE = "Java";

    public JavaCodeGenerator(@NotNull RESOLVECompiler compiler,
                             @NotNull AnnotatedModule module) {
        super(compiler, module, LANGUAGE);
    }

    @NotNull private OutputModelObject buildModuleOutputModel() {
        ModelBuilder builder = new ModelBuilder(this, getCompiler().symbolTable);
        ParseTree root = getModule().getRoot();
        ParseTreeWalker.DEFAULT.walk(builder, root);
        return builder.built.get(root);
    }

    public ST generateModule() {
        return walk(buildModuleOutputModel());
    }
}