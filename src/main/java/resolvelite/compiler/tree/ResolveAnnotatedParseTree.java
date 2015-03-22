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
package resolvelite.compiler.tree;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import resolvelite.misc.Utils.Builder;
import resolvelite.parsing.ResolveParser;
import resolvelite.semantics.MathType;

public class ResolveAnnotatedParseTree {

    @NotNull private final String name, fileName;
    @NotNull private final boolean hasErrors;
    @NotNull private final ParseTree root;
    @NotNull private final ImportCollection imports;

    private ResolveAnnotatedParseTree(@NotNull TreeAnnotatingBuilder builder) {
        this.root = builder.root;
        this.imports = builder.imports;
        this.name = builder.name.getText();
        this.hasErrors = builder.hasErrors;
        this.fileName = builder.fileName;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public String getFileName() {
        return fileName;
    }

    @NotNull
    public ImportCollection getImportCollection() {
        return imports;
    }

    @NotNull
    public ParseTree getRoot() {
        return root;
    }

    public static class TreeAnnotatingBuilder
            implements
                Builder<ResolveAnnotatedParseTree> {

        public Token name;
        public final String fileName;
        public boolean hasErrors;
        public final ParseTree root;
        public final ImportCollection imports;

        public TreeAnnotatingBuilder(ParseTree root, String fileName) {
            if ( !(root instanceof ResolveParser.ModuleContext) ) {
                throw new IllegalArgumentException(
                        "ResolveParser.ModuleContext " + "expected, got: "
                                + root.getClass());
            }
            ImportListener scanner = new ImportListener();
            ParseTreeWalker.DEFAULT.walk(scanner, root);
            this.fileName = fileName;
            this.imports = scanner.getImports();
            this.root = root;
            ParseTree child = root.getChild(0);
            if ( child instanceof ResolveParser.PrecisModuleContext ) {
                this.name = ((ResolveParser.PrecisModuleContext) child).name;
            }
            else if ( child instanceof ResolveParser.ConceptModuleContext ) {
                this.name = ((ResolveParser.ConceptModuleContext) child).name;
            }
            else if ( child instanceof ResolveParser.FacilityModuleContext ) {
                this.name = ((ResolveParser.FacilityModuleContext) child).name;
            }
            else {
                throw new IllegalArgumentException("Unrecognized module");
            }
        }

        public TreeAnnotatingBuilder hasErrors(boolean e) {
            this.hasErrors = e;
            return this;
        }

        @Override
        public ResolveAnnotatedParseTree build() {
            return new ResolveAnnotatedParseTree(this);
        }
    }
}
