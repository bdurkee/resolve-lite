package edu.clemson.resolve.compiler;

import org.antlr.v4.tool.ErrorSeverity;

public enum ErrorKind {

    /**
     * Compiler Error 1: cannot write file <em>filename</em>: <em>reason</em>.
     */
    CANNOT_WRITE_FILE(1, "cannot write file <arg>: <arg2>", ErrorSeverity.ERROR),

    /**
     * Compiler Error 2: cannot find or open file: <em>filename</em>.
     */
    CANNOT_OPEN_FILE(2, "cannot find or open file: <arg>"
            + "<if(exception&&verbose)>; reason: <exception><endif>",
            ErrorSeverity.ERROR),

    /**
     * Compiler Error 3: invalid command-line argument: <em>argument</em>.
     */
    INVALID_CMDLINE_ARG(3, "invalid command line argument: <arg>",
            ErrorSeverity.ERROR),

    /**
     * Compiler Error 4: directory not found: <em>directory</em>
     */
    DIR_NOT_FOUND(4, "directory not found: <arg>", ErrorSeverity.ERROR),

    /**
     * Compiler Error 5: output directory is a file: <em>filename</em>
     */
    OUTPUT_DIR_IS_FILE(5, "output directory is a file: <arg>",
            ErrorSeverity.ERROR),

    /**
     * Compiler Error 6: internal error: <em>message</em>.
     */
    INTERNAL_ERROR(6, "internal error: <arg> <arg2><if(exception&&verbose)>: "
                           + "<exception>" + "<stackTrace; separator=\"\\n\"><endif>",
                   ErrorSeverity.ERROR),

    /**
     * Compiler Error 7: syntax error: <em>errormsg</em>.
     */
    SYNTAX_ERROR(7, "syntax error: <arg>", ErrorSeverity.ERROR),

    /**
     * Compiler Error 8: module <em>name</em> was unable to find the file
     * corresponding to uses reference '<em>usesref</em>'.
     */
    MISSING_IMPORT_FILE(8, "module <arg> was unable to find the file "
            + "corresponding to uses reference '<arg2>'",
            ErrorSeverity.ERROR),

    /**
     * Compiler Error 9: circular dependency: <em>moduleName1</em> depends on
     * <em>moduleName2</em>, but <em>moduleName2</em> also depends on
     * <em>moduleName1</em>.
     */
    CIRCULAR_DEPENDENCY(9, "circular dependency: <arg> depends on <arg2>, but "
            + "<arg2> also depends on <arg>",
            ErrorSeverity.ERROR),

    /**
     * Compiler Error 10: no such symbol: <em>name</em> in <em>moduleName</em>.
     */
    NO_SUCH_SYMBOL(10, "no such symbol: <arg> <if(arg2)>in <arg2><endif>",
            ErrorSeverity.ERROR),

    /**
     * Compiler Error 11: no such module: <em>moduleName</em>.
     */
    NO_SUCH_MODULE(11, "no such module: <arg>", ErrorSeverity.ERROR),

    /**
     * Compiler Error 12: duplicate symbol: <em>name</em>.
     */
    DUP_SYMBOL(12, "duplicate symbol: <arg>", ErrorSeverity.ERROR),

    /**
     * Compiler Error 13: expecting <em>entrytype</em>: found
     * <em>foundentrytype</em>.
     */
    UNEXPECTED_SYMBOL(13, "expecting <arg> on '<arg2>', found <arg3> instead",
            ErrorSeverity.ERROR),

    /**
     * Compiler Error 14: <em>typename</em> is not known to be a type.
     */
    INVALID_MATH_TYPE(14, "'<arg>' is not known to be a type",
                      ErrorSeverity.ERROR),

    /**
     * Compiler Error 15: no such function: <em>name</em>.
     */
    NO_SUCH_MATH_FUNCTION(15, "no such function: <arg>", ErrorSeverity.ERROR),

    /**
     * Compiler Error 16: no function applicable for domain <em>domain</em>;
     * candidates: <em>candidatename</em> : <em>candidatetype</em>.
     */
    NO_MATH_FUNC_FOR_DOMAIN(
            16,
            "no function applicable for domain <arg>;"
                    + "\ncandidates: "
                    + "<arg2, arg3 : {name,domain|('<name.name>' : <domain>)};separator={,\n}>",
            ErrorSeverity.ERROR),

    /**
     * Compiler Error 17: multiple domain matches: <em>match1name</em> :
     * <em>match1type</em> and <em>match2name</em> : <em>match2type</em>;
     * consider explicitly qualifying.
     */
    AMBIGIOUS_DOMAIN(17, "multiple domain matches; for example: "
            + "<arg> : <arg2>  and  <arg3> : <arg4> \nconsider explicitly "
            + "qualifying", ErrorSeverity.WARNING),

    /**
     * Compiler Error 18: expected: <em>type</em>; found <em>othertype</em>.
     */
    UNEXPECTED_TYPE(18, "expected: <arg>; found: <arg2>", ErrorSeverity.ERROR),

    /**
     * Compiler Error 19: value on seg <em>name</em> is not a tuple.
     */
    VALUE_NOT_TUPLE(19, "value on seg <arg> is not a tuple",
            ErrorSeverity.ERROR),

    /**
     * Compiler Error 20: no such factor <em>name</em>.
     */
    NO_SUCH_FACTOR(20, "no such factor <arg>", ErrorSeverity.ERROR);

    public final int code;
    public final String message;

    public final ErrorSeverity severity;

    ErrorKind(int code, String message, ErrorSeverity severity) {
        this.code = code;
        this.message = message;
        this.severity = severity;
    }
}