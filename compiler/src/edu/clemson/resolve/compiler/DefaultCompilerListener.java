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
package edu.clemson.resolve.compiler;

import edu.clemson.resolve.RESOLVECompiler;
import org.jetbrains.annotations.NotNull;
import org.stringtemplate.v4.ST;

/**
 * A basic, default implementation of a listener for the compiler that reports
 * warnings, errors, and other miscellaneous info.
 * <p>
 * Note that this implementation simply outputs the information
 * received directly to {@code stdout} (or {@code stderr}); it doesn't try to
 * save or otherwise preserve any of the information forwarded.</p>
 */
public class DefaultCompilerListener implements RESOLVECompilerListener {

    private final RESOLVECompiler compiler;

    public DefaultCompilerListener(@NotNull RESOLVECompiler c) {
        this.compiler = c;
    }

    @Override
    public void error(@NotNull RESOLVEMessage msg) {
        ST msgST = compiler.errMgr.getMessageTemplate(msg);
        String outputMsg = msgST.render();
        if (compiler.errMgr.formatWantsSingleLineMessage()) {
            outputMsg = outputMsg.replace('\n', ' ');
        }
        System.err.println(outputMsg);
    }

    @Override
    public void info(@NotNull String msg) {
        if (compiler.errMgr.formatWantsSingleLineMessage()) {
            msg = msg.replace('\n', ' ');
        }
        System.out.println(msg);
    }

    @Override
    public void warning(@NotNull RESOLVEMessage msg) {
        ST msgST = compiler.errMgr.getMessageTemplate(msg);
        String outputMsg = msgST.render();
        if (compiler.errMgr.formatWantsSingleLineMessage()) {
            outputMsg = outputMsg.replace('\n', ' ');
        }
        System.err.println(outputMsg);
    }
}
