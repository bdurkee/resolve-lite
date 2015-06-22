package edu.clemson.resolve.compiler;

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
     * Compiler error 4: directory not found: <em>directory</em>
     */
    DIR_NOT_FOUND(4, "directory not found: <arg>", ErrorSeverity.ERROR),

    /**
     * Compiler error 5: output directory is a file: <em>filename</em>
     */
    OUTPUT_DIR_IS_FILE(5, "output directory is a file: <arg>",
            ErrorSeverity.ERROR),

    /**
     * Compiler error 6: internal error: <em>message</em>.
     */
    INTERNAL_ERROR(6, "internal error: <arg> <arg2><if(exception&&verbose)>: "
                           + "<exception>" + "<stackTrace; separator=\"\\n\"><endif>",
                   ErrorSeverity.ERROR),

    /**
     * Compiler error 7: syntax error: <em>errormsg</em>.
     */
    SYNTAX_ERROR(7, "syntax error: <arg>", ErrorSeverity.ERROR),

    /**
     * Compiler Error 8: <em>module1type</em> module '<em>module1</em>' cannot
     * import <em>module2type</em> module '<em>module2</em>'.
     */
    MISSING_IMPORT_FILE(8, "module <arg> was unable to find the file "
            + "corresponding to import reference '<arg2>'",
            ErrorSeverity.ERROR),

    /**
     * Compiler Error 9: circular dependency: <em>moduleName1</em> depends on
     * <em>moduleName2</em>, but <em>moduleName2</em> also depends on
     * <em>moduleName1</em>.
     */
    CIRCULAR_DEPENDENCY(9, "circular dependency: <arg> depends on <arg2>, but "
            + "<arg2> also depends on <arg>",
            ErrorSeverity.ERROR);

    public final int code;
    public final String message;

    public final ErrorSeverity severity;

    ErrorKind(int code, String message, ErrorSeverity severity) {
        this.code = code;
        this.message = message;
        this.severity = severity;
    }
}