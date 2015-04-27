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
package org.resolvelite.compiler.tree;

import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.resolvelite.parsing.ResolveParser;
import org.resolvelite.semantics.MTType;
import org.resolvelite.semantics.programtype.PTType;

public class AnnotatedTree {

    public ParseTreeProperty<MTType> mathTypes = new ParseTreeProperty<>();
    public ParseTreeProperty<MTType> mathTypeValues = new ParseTreeProperty<>();
    public ParseTreeProperty<PTType> progTypes = new ParseTreeProperty<>();
    public ParseTreeProperty<PTType> progTypeValues = new ParseTreeProperty<>();

    private final String name, fileName;
    private final ParseTree root;
    public boolean hasErrors;
    public ImportCollection imports;

    public AnnotatedTree(@NotNull ParseTree root, @NotNull String name,
            String fileName, boolean hasErrors) {
        this.hasErrors = hasErrors;
        this.root = root;
        this.name = name;
        this.fileName = fileName;

        //if we have syntactic errors, better not risk processing imports with
        //our tree (we usually get npe's).
        if ( !hasErrors ) {
            ImportListener l = new ImportListener();
            ParseTreeWalker.DEFAULT.walk(l, root);
            this.imports = l.getImports();
        }
        else {
            this.imports = new ImportCollection();
        }
    }

    @NotNull public String getName() {
        return name;
    }

    @NotNull public String getFileName() {
        return fileName;
    }

    @NotNull public ParseTree getRoot() {
        return root;
    }

    @Override public boolean equals(Object o) {
        boolean result = (o instanceof AnnotatedTree);
        if ( result ) {
            result = this.name.equals(((AnnotatedTree) o).name);
        }
        return result;
    }
}
