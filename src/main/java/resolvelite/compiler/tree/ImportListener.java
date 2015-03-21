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

import org.antlr.runtime.tree.ParseTree;
import org.antlr.v4.runtime.misc.NotNull;
import resolvelite.parsing.ResolveBaseListener;
import resolvelite.parsing.ResolveParser;

/**
 * Fills in the contents of an {@link ImportCollection} by visiting the
 * various {@link ParseTree} nodes that reference other modules.
 */
public class ImportListener extends ResolveBaseListener {

    private final ImportCollection.ImportCollectionBuilder builder =
            new ImportCollection.ImportCollectionBuilder();

    @NotNull
    public ImportCollection getImports() {
        return builder.build();
    }

    //Todo: override facilities, enhancements, etc when they get added to the
    //grammar.
    @Override
    public void exitImportList(@NotNull ResolveParser.ImportListContext ctx) {
        builder.imports(ImportCollection.ImportType.EXPLICIT, ctx.Identifier());
    }
}
