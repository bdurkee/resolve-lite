package edu.clemson.resolve.codegen;

import edu.clemson.resolve.codegen.model.OutputModelObject;
import edu.clemson.resolve.compiler.AnnotatedModule;
import edu.clemson.resolve.compiler.ErrorKind;
import edu.clemson.resolve.compiler.RESOLVECompiler;
import edu.clemson.resolve.misc.Utils;
import org.jetbrains.annotations.NotNull;
import org.stringtemplate.v4.*;
import org.stringtemplate.v4.misc.STMessage;

public abstract class AbstractCodeGenerator {

    public static final String TEMPLATE_ROOT =
            "edu/clemson/resolve/templates/codegen";

    @NotNull protected final RESOLVECompiler compiler;
    @NotNull protected final AnnotatedModule module;
    @NotNull private final STGroup templates;
    @NotNull private final String language;

    public AbstractCodeGenerator(@NotNull RESOLVECompiler rc,
                                 @NotNull AnnotatedModule module,
                                 @NotNull String language) {
        this.compiler = rc;
        this.module = module;
        this.language = language;
        this.templates = loadTemplates();
    }

    @NotNull public AnnotatedModule getModule() {
        return module;
    }

    @NotNull public RESOLVECompiler getCompiler() {
        return compiler;
    }

    @NotNull protected ST walk(@NotNull OutputModelObject outputModel) {
        ModelConverter walker = new ModelConverter(compiler, templates);
        return walker.walk(outputModel);
    }

    @NotNull public String getFileName() {
        ST extST = templates.getInstanceOf("fileExtension");
        String moduleName = module.getName();
        return moduleName + extST.render();
    }

    public void writeFile(ST outputFileST) {
        Utils.writeFile(compiler.outputDirectory, getFileName(),
                outputFileST.render());
    }

    public STGroup loadTemplates() {
        String groupFileName = TEMPLATE_ROOT + "/" + language +
                STGroup.GROUP_FILE_EXTENSION;
        STGroup result = null;
        try {
            result = new STGroupFile(groupFileName);
        }
        catch (IllegalArgumentException iae) {
            compiler.errMgr.toolError(
                    ErrorKind.MISSING_CODE_GEN_TEMPLATES, iae, "Java");
        }
        if ( result == null ) {
            return null;
        }
        result.registerRenderer(Integer.class, new NumberRenderer());
        result.registerRenderer(String.class, new StringRenderer());
        result.setListener(new STErrorListener() {

            @Override public void compileTimeError(STMessage msg) {
                reportError(msg);
            }

            @Override public void runTimeError(STMessage msg) {
                reportError(msg);
            }

            @Override public void IOError(STMessage msg) {
                reportError(msg);
            }

            @Override public void internalError(STMessage msg) {
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
