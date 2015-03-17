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

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.TokenSource;
import org.antlr.v4.runtime.misc.Pair;
import resolvelite.parsing.ResolveLexer;

/**
 * <p>
 * A special token that overrides the "equals" logic present in the default
 * implementation of {@link org.antlr.v4.runtime.CommonToken}. Turns out this is
 * functionally equivalent to our now removed <tt>PosSymbol</tt> class.
 * </p>
 */
public class ResolveToken extends CommonToken {

    public String mySourceName;

    public ResolveToken(String text) {
        super(ResolveLexer.Identifier, text);
    }

    public ResolveToken(int type, String text) {
        super(type, text);
    }

    public ResolveToken(Pair<TokenSource, CharStream> source, int type,
            int channel, int start, int stop) {
        super(source, type, channel, start, stop);
    }

    @Override
    public String toString() {
        return getText();
    }

    @Override
    public int hashCode() {
        return getText().hashCode();
    }

    public boolean equals(Object o) {
        boolean result = (o instanceof ResolveToken);

        if (result) {
            result = ((ResolveToken) o).getText().equals(getText());
        }
        return result;
    }
}