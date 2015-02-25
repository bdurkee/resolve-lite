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

import org.antlr.v4.tool.ErrorSeverity;

public enum ErrorKind {

    /**
     * <p>Compiler error 1: cannot write file <em>filename</em>:
     * <em>reason</em></p>
     */
    CANNOT_WRITE_FILE(1, "cannot write file <arg>: <arg2>",
            ErrorSeverity.ERROR),

    /**
     * <p>Compiler error 2: cannot find or open file: <em>filename</em></p>
     */
    CANNOT_OPEN_FILE(2, "cannot find or open file: <arg>"
            + "<if(exception&&verbose)>; reason: <exception><endif>",
            ErrorSeverity.ERROR),

    /**
     * <p>Compiler error 3: invalid command-line argument:
     * <em>argument</em>.</p>
     */
    INVALID_CMDLINE_ARG(3, "invalid command line argument: <arg>",
            ErrorSeverity.ERROR),

    /**
     * <p>Compiler error 4: internal error: <em>message</em>.</p>
     */
    INTERNAL_ERROR(4, "internal error: <arg> <arg2><if(exception&&verbose)>: " +
            "<exception>" +
            "<stackTrace; separator=\"\\n\"><endif>", ErrorSeverity.ERROR),

    /**
     * <p>Compiler error 5: syntax error: <em>errormsg</em>.</p>
     */
    SYNTAX_ERROR(5, "syntax error: <arg>", ErrorSeverity.ERROR);

    public final int code;
    public final String message;

    public final ErrorSeverity severity;

    ErrorKind(int code, String message, ErrorSeverity severity) {
        this.code = code;
        this.message = message;
        this.severity = severity;
    }
}
