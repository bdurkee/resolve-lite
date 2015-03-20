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

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import resolvelite.misc.Utils.Builder;
import resolvelite.parsing.ResolveParser;
import resolvelite.semantics.MTType;

import java.io.File;

public class ResolveAnnotatedParseTree {

    @NotNull private final ParseTree root;
    @NotNull private final ImportCollection imports;
    @NotNull private final ParseTreeProperty<MTType> mathTypes, mathTypeValues;

    private ResolveAnnotatedParseTree(@NotNull TreeAnnotatingBuilder builder) {
        this.root = builder.root;
        this.mathTypes = builder.mathTypes;
        this.mathTypeValues = builder.mathTypeValues;
        this.imports = builder.imports;
    }

    @NotNull
    public MTType getMathType(@NotNull ParseTree t) {
        return mathTypes.get(t);
    }

    @NotNull
    public MTType getMathTypeValue(@NotNull ParseTree t) {
        return mathTypeValues.get(t);
    }

    public static class TreeAnnotatingBuilder
            implements
                Builder<ResolveAnnotatedParseTree> {

        public Token name;
        public boolean hasErrors;
        public final ParseTree root;
        public final ImportCollection imports;
        public final ParseTreeProperty<MTType> mathTypes =
                new ParseTreeProperty<MTType>();
        public final ParseTreeProperty<MTType> mathTypeValues =
                new ParseTreeProperty<MTType>();

        public TreeAnnotatingBuilder(ParseTree root, String fileName) {
            if ( !(root instanceof ResolveParser.ModuleContext) ) {
                throw new IllegalArgumentException(
                        "ResolveParser.ModuleContext " + "expected, got: "
                                + root.getClass());
            }
            ImportListener scanner = new ImportListener();
            ParseTreeWalker.DEFAULT.walk(scanner, root);
            this.imports = scanner.getImports();
            this.root = root;
            ParseTree child = root.getChild(0);
            if ( child instanceof ResolveParser.PrecisModuleContext ) {
                this.name = ((ResolveParser.PrecisModuleContext) child).name;
            }
            else if ( child instanceof ResolveParser.ConceptModuleContext ) {
                this.name = ((ResolveParser.ConceptModuleContext) child).name;
            }
        }

        public TreeAnnotatingBuilder hasErrors(boolean e) {
            this.hasErrors = e;
            return this;
        }

        public TreeAnnotatingBuilder setMathType(@NotNull ParseTree ctx,
                @NotNull MTType type) {
            mathTypes.put(ctx, type);
            return this;
        }

        public TreeAnnotatingBuilder setMathTypeValue(@NotNull ParseTree ctx,
                @NotNull MTType typeValue) {
            mathTypeValues.put(ctx, typeValue);
            return this;
        }

        @Override
        public ResolveAnnotatedParseTree build() {
            return new ResolveAnnotatedParseTree(this);
        }
    }
}
