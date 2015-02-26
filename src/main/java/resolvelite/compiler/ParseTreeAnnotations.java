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
package resolvelite.compiler;

import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import resolvelite.misc.Builder;
import resolvelite.typeandpopulate.MTType;

import java.io.File;

public class ParseTreeAnnotations {

    private final ParseTree root;
    //Not sure if this should really go here. We'll see.
    private final File file;
    private final ParseTreeProperty<MTType> mathTypes;
    private final ParseTreeProperty<MTType> mathTypeValues;

    private ParseTreeAnnotations(TreeAnnotatingBuilder builder) {
        this.root = builder.root;
        this.mathTypes = builder.mathTypes;
        this.mathTypeValues = builder.mathTypeValues;
        this.file = builder.file;
    }

    @NotNull
    public File getFile() {
        return file;
    }

    @NotNull
    public MTType getMathType(@NotNull ParseTree t) {
        return mathTypes.get(t);
    }

    @NotNull
    public MTType getMathTypeValue(@NotNull ParseTree t) {
        return mathTypeValues.get(t);
    }

    @NotNull
    public ParseTree getRoot() {
        return root;
    }

    public static class TreeAnnotatingBuilder
            implements
                Builder<ParseTreeAnnotations> {

        protected final ParseTreeProperty<MTType> mathTypes =
                new ParseTreeProperty<MTType>();
        protected final ParseTreeProperty<MTType> mathTypeValues =
                new ParseTreeProperty<MTType>();
        protected ParseTree root;
        protected File file;

        public TreeAnnotatingBuilder(ParseTree root, @NotNull File file) {
            this.root = root;
            this.file = file;
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
        public ParseTreeAnnotations build() {
            return new ParseTreeAnnotations(this);
        }
    }
}
