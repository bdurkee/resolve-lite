/*
 * [The "BSD license"]
 * Copyright (c) 2015 Clemson University
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 
 * 3. The name of the author may not be used to endorse or promote products
 * derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package resolvelite.codegen;

import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.misc.Nullable;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.stringtemplate.v4.*;
import org.stringtemplate.v4.misc.STMessage;
import resolvelite.codegen.model.OutputModelObject;
import resolvelite.compiler.ErrorKind;
import resolvelite.compiler.ResolveCompiler;
import resolvelite.compiler.tree.ResolveAnnotatedParseTree.TreeAnnotatingBuilder;

import java.io.IOException;
import java.io.Writer;

public class CodeGenerator {

    public static final String TEMPLATE_ROOT = "codegen";

    public static final String DEFAULT_LANGUAGE = "java";

    protected final ResolveCompiler compiler;
    protected final TreeAnnotatingBuilder module;
    private final STGroup templates;

    public final int myLineWidth = 72;

    public CodeGenerator(@NotNull ResolveCompiler rc,
            @NotNull TreeAnnotatingBuilder current) {
        this.compiler = rc;
        this.module = current;
        this.templates = loadTemplates();
    }

    private OutputModelObject buildModuleOutputModel() {
        ModelBuilder builder = new ModelBuilder(this, compiler.symbolTable);
        ParseTree root = module.root;
        ParseTreeWalker.DEFAULT.walk(builder, root);
        return builder.built.get(root);
    }

    private ST walk(OutputModelObject outputModel) {
        ModelConverter walker = new ModelConverter(compiler, templates);
        return walker.walk(outputModel);
    }

    @Nullable public ST generateModule() {
        return walk(buildModuleOutputModel());
    }

    @NotNull public TreeAnnotatingBuilder getModule() {
        return module;
    }

    @NotNull public ResolveCompiler getCompiler() {
        return compiler;
    }

    public void writeModuleFile(ST outputFileST) {
        write(outputFileST, getModuleFileName());
    }

    private void write(ST code, String fileName) {
        try {
            Writer w = null; //myTool.getOutputFileWriter(g, fileName);
            STWriter wr = new AutoIndentWriter(w);
            wr.setLineWidth(myLineWidth);
            code.write(wr);
            w.close();
        }
        catch (IOException ioe) {
            compiler.errorManager.toolError(ErrorKind.CANNOT_WRITE_FILE, ioe,
                    fileName);
        }
    }

    public String getModuleFileName() {
        ST extST = templates.getInstanceOf("moduleFileExtension");
        String moduleName = module.name.getText();
        return moduleName + extST.render();
    }

    @Nullable protected STGroup loadTemplates() {
        String groupFileName =
                CodeGenerator.TEMPLATE_ROOT + "/" + DEFAULT_LANGUAGE + "/"
                        + DEFAULT_LANGUAGE + STGroup.GROUP_FILE_EXTENSION;
        STGroup result = null;
        try {
            result = new STGroupFile(groupFileName);
        }
        catch (IllegalArgumentException iae) {
            compiler.errorManager
                    .toolError(ErrorKind.MISSING_CODE_GEN_TEMPLATES, iae,
                            DEFAULT_LANGUAGE);
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
                compiler.errorManager.toolError(
                        ErrorKind.STRING_TEMPLATE_WARNING, msg.cause,
                        msg.toString());
            }
        });
        return result;
    }
}
