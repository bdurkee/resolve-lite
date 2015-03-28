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
     * Compiler error 1: cannot write file <em>filename</em>: <em>reason</em>.
     */
    CANNOT_WRITE_FILE(1, "cannot write file <arg>: <arg2>", ErrorSeverity.ERROR),

    /**
     * Compiler error 2: cannot find or open file: <em>filename</em>.
     */
    CANNOT_OPEN_FILE(2, "cannot find or open file: <arg>"
            + "<if(exception&&verbose)>; reason: <exception><endif>",
            ErrorSeverity.ERROR),

    /**
     * Compiler error 3: invalid command-line argument: <em>argument</em>.
     */
    INVALID_CMDLINE_ARG(3, "invalid command line argument: <arg>",
            ErrorSeverity.ERROR),

    /**
     * Compiler error 4: internal error: <em>message</em>.
     */
    INTERNAL_ERROR(4, "internal error: <arg> <arg2><if(exception&&verbose)>: "
            + "<exception>" + "<stackTrace; separator=\"\\n\"><endif>",
            ErrorSeverity.ERROR),

    /**
     * Compiler error 5: syntax error: <em>errormsg</em>.
     */
    SYNTAX_ERROR(5, "syntax error: <arg>", ErrorSeverity.ERROR),

    /**
     * Compiler error 6: unexpected symboltablentry: <em>expectedentry</em>,
     * <em>foundentry</em>.
     */
    UNEXPECTED_SYMBOL(6, "expecting <arg>; found <arg2>", ErrorSeverity.ERROR),

    /**
     * Compiler error 7: duplicate symbol: <em>name</em>.
     */
    DUP_SYMBOL(7, "duplicate symbol: <arg>", ErrorSeverity.ERROR),

    /**
     * Compiler warning 8: mismatched block top and bottom names.
     */
    MISMATCHED_BLOCK_NAMES(8, "block start name '<arg>' does not match the "
            + "declared closing name '<arg2>'", ErrorSeverity.WARNING),

    /**
     * Compiler error 9: no such symbol: <em>name</em> in <em>moduleName</em>
     */
    NO_SUCH_SYMBOL(9, "no such symbol: <arg> <if(arg2)>in <arg2><endif>",
            ErrorSeverity.ERROR),

    /**
     * Compiler Error 10: <em>module1type</em> module '<em>module1</em>' cannot
     * import <em>module2type</em> module '<em>module2</em>'.
     */
    INVALID_IMPORT(10, "<arg> module '<arg1>' cannot import "
            + "<arg2> module '<arg3>'", ErrorSeverity.ERROR),

    /**
     * Compiler Error 11: <em>module1type</em> module '<em>module1</em>' cannot
     * import <em>module2type</em> module '<em>module2</em>'.
     */
    MISSING_IMPORT_FILE(11, "unable to find the file corresponding to "
            + "import reference: '<arg>'", ErrorSeverity.ERROR),

    /**
     * Compiler error 12: no mapping to template name for output model
     * class <em>class</em>.
     */
    NO_MODEL_TO_TEMPLATE_MAPPING(12, "no mapping to template name for output "
            + "model class <arg>", ErrorSeverity.ERROR),

    /**
     * Compiler error 13: missing code generation template <em>template</em>.
     */
    CODE_GEN_TEMPLATES_INCOMPLETE(13, "missing code generation template <arg>",
            ErrorSeverity.ERROR),

    /**
     * Compiler error 14: code generation template <em>template</em> has
     * missing, misnamed, or incomplete arg list; missing <em>field</em>.
     */
    CODE_TEMPLATE_ARG_ISSUE(14, "code generation template <arg> has missing, "
            + "misnamed, or incomplete arg list; missing <arg2>",
            ErrorSeverity.ERROR),

    /**
     * Compiler error 15: can't find code generation templates: <em>group</em>.
     */
    MISSING_CODE_GEN_TEMPLATES(15,
            "can't find code generation templates: <arg>", ErrorSeverity.ERROR),

    /**
     * Compiler warning 16: template error: <em>message</em>.
     */
    STRING_TEMPLATE_WARNING(16, "template error: <arg> "
            + "<arg2><if(exception&&verbose)>: <exception>"
            + "<stackTrace; separator=\"\\n\"><endif>", ErrorSeverity.WARNING),

    /**
     * Compiler error 17: RESOLVE cannot generate <em>language</em> code as
     * of version <em>version</em>.
     */
    CANNOT_CREATE_TARGET_GENERATOR(17, "RESOLVE cannot generate <arg> code as "
            + "of version " + ResolveCompiler.VERSION,
            ErrorSeverity.ERROR_ONE_OFF),

    /**
     * Compiler error 18: Non-facility qualifier on variable: <em>varName</em>.
     */
    NON_FACILITY_QUALIFIER(18, "qualifier '<arg>' refers"
            + " to a module, not a facility", ErrorSeverity.ERROR),

    /**
     * Compiler error 19: type <em>name</em> is not known to ba type".
     */
    INVALID_MATH_TYPE(19, "type '<arg>' is not known to be a type",
            ErrorSeverity.ERROR),

    INCOMPATIBLE_TYPES(20, "<arg>:<arg2>, <arg3>:<arg4> have incompatible "
            + "program types in <arg5>", ErrorSeverity.ERROR),

    NO_SUCH_MODULE(21, "no such module: <arg>", ErrorSeverity.ERROR);

    public final int code;
    public final String message;

    public final ErrorSeverity severity;

    ErrorKind(int code, String message, ErrorSeverity severity) {
        this.code = code;
        this.message = message;
        this.severity = severity;
    }
}
