package edu.clemson.resolve.codegen;

import edu.clemson.resolve.codegen.Model.OutputModelObject;
import edu.clemson.resolve.compiler.AnnotatedModule;
import edu.clemson.resolve.compiler.ErrorKind;
import edu.clemson.resolve.RESOLVECompiler;
import edu.clemson.resolve.semantics.ModuleIdentifier;
import org.jetbrains.annotations.NotNull;
import org.stringtemplate.v4.*;
import org.stringtemplate.v4.misc.STMessage;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.function.Function;

/**
 * A general base class for anything in the compiler that requires us to produce a 'significant' amount of structured
 * code/output/text. This includes our Java code generator {@link JavaCodeGenerator}, as well as VCs produced
 * from {@link edu.clemson.resolve.vcgen.VCGenerator}.
 */
public abstract class AbstractCodeGenerator {

    public static final String TEMPLATE_ROOT = "edu/clemson/resolve/templates/codegen";

    protected final RESOLVECompiler compiler;
    protected final AnnotatedModule module;
    protected final STGroup templates;
    private final String language;

    public AbstractCodeGenerator(@NotNull RESOLVECompiler rc,
                                 @NotNull AnnotatedModule module,
                                 @NotNull String language) {
        this.compiler = rc;
        this.module = module;
        this.language = language;
        this.templates = loadTemplates();
    }

    @NotNull
    public AnnotatedModule getModule() {
        return module;
    }

    @NotNull
    public RESOLVECompiler getCompiler() {
        return compiler;
    }

    @NotNull
    protected ST walk(@NotNull OutputModelObject outputModel) {
        ModelConverter walker = new ModelConverter(compiler, templates);
        return walker.walk(outputModel);
    }

    @NotNull
    String getOutputFileName() {
        String moduleName = module.getNameToken().getText();
        return moduleName + getFileExtension();
    }

    @NotNull
    String getFileExtension() {
        ST extST = templates.getInstanceOf("fileExtension");
        if (extST == null) {
            throw new IllegalStateException("forgot to define template for" +
                    "language file extension " +
                    "(for example: fileExtension() ::= '.java'))");
        }
        return extST.render();
    }

    public void write(@NotNull ST code, @NotNull String outputFileName) {
        write(module.getModuleIdentifier(), code, outputFileName);
    }

    protected void write(@NotNull ModuleIdentifier moduleIdentifier,
                         @NotNull ST code,
                         @NotNull String outputFileName) {
        try {
            Writer w = compiler.getOutputFileWriter(moduleIdentifier, outputFileName);
            STWriter wr = new AutoIndentWriter(w);
            wr.setLineWidth(80);
            code.write(wr);
            w.close();
        } catch (IOException ioe) {
            compiler.errMgr.toolError(ErrorKind.CANNOT_WRITE_FILE,
                    ioe,
                    outputFileName);
        }
    }

    private STGroup loadTemplates() {
        String groupFileName = TEMPLATE_ROOT + "/" + language + STGroup.GROUP_FILE_EXTENSION;
        STGroup result = null;
        try {
            result = new STGroupFile(groupFileName);
        } catch (IllegalArgumentException iae) {
            compiler.errMgr.toolError(
                    ErrorKind.MISSING_CODE_GEN_TEMPLATES, iae, "Java");
        }
        if (result == null) {
            return null;
        }
        result.registerRenderer(Integer.class, new NumberRenderer());
        result.registerRenderer(String.class, new StringRenderer());
        result.setListener(new STErrorListener() {

            @Override
            public void compileTimeError(STMessage msg) {
                reportError(msg);
            }

            @Override
            public void runTimeError(STMessage msg) {
                reportError(msg);
            }

            @Override
            public void IOError(STMessage msg) {
                reportError(msg);
            }

            @Override
            public void internalError(STMessage msg) {
                reportError(msg);
            }

            private void reportError(STMessage msg) {
                compiler.errMgr.toolError(
                        ErrorKind.STRING_TEMPLATE_WARNING, msg.cause,
                        msg.toString());
            }
        });
        return result;
    }
}
