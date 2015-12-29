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
     * Compiler Error 13: expecting <em>expectedSymbolType</em>: found
     * <em>actualSymbolType</em>.
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
    UNEXPECTED_TYPE(18, "expected: <arg>, found: <arg2>", ErrorSeverity.ERROR),

    /**
     * Compiler Error 19: value on seg <em>name</em> is not a tuple.
     */
    VALUE_NOT_TUPLE(19, "value on seg <arg> is not a tuple",
            ErrorSeverity.ERROR),

    /**
     * Compiler Error 20: no such factor <em>name</em>.
     */
    NO_SUCH_FACTOR(20, "no such factor <arg> (perhaps trying to access" +
            " something that isn't of type cartesian?)", ErrorSeverity.ERROR),

    /**
     * Compiler Error 21: procedure <em>name</em> does not implement any known
     * operation.
     */
    DANGLING_PROCEDURE(21, "procedure <arg> does not implement any known "
            + "operation", ErrorSeverity.ERROR),

    /**
     * Compiler Error 22: "illegal member access expression: <em>exp</em>;
     * <em>exp first-component</em> must refer to a record".
     */
    ILLEGAL_MEMBER_ACCESS(22, "illegal member access: " +
            "either <arg> is not a record/aggregate type, or <arg2> is a " +
            "missing (undeclared) field.", ErrorSeverity.ERROR),

    /**
     * Compiler Error 30: no operation found corresponding to call
     * <em>callname</em> with the following arguments: <em>arg : type ...</em>"
     */
    NO_SUCH_OPERATION(30, "no operation found corresponding to call "
            + "<arg> <if(arg2)>" + "with the following arguments: "
            + "[<arg2, arg3 : {name,type| <name> : <type>}; separator={,\n}>]"
            + "<endif>", ErrorSeverity.ERROR),

    /**
     * Compiler Error 31:
     * <p>
     * no mapping to template name for output model class <em>class</em>.</p>
     */
    NO_MODEL_TO_TEMPLATE_MAPPING(31, "no mapping to template name for output "
            + "model class <arg>", ErrorSeverity.ERROR),

    /**
     * Compiler Error 32: missing code generation template <em>template</em>.
     */
    CODE_GEN_TEMPLATES_INCOMPLETE(32, "missing code generation template <arg>",
            ErrorSeverity.ERROR),

    /**
     * Compiler Error 33: code generation template <em>template</em> has
     * missing, misnamed, or incomplete arg list; missing <em>field</em>.
     */
    CODE_TEMPLATE_ARG_ISSUE(33, "code generation template <arg> has missing, "
            + "misnamed, or incomplete arg list; missing <arg2>",
            ErrorSeverity.ERROR),

    /**
     * Compiler Error 34: can't find code generation templates: <em>group</em>.
     */
    MISSING_CODE_GEN_TEMPLATES(34,
            "can't find code generation templates: <arg>", ErrorSeverity.ERROR),

    /**
     * Compiler Warning 35: template error: <em>message</em>.
     */
    STRING_TEMPLATE_WARNING(35, "template error: <arg> "
            + "<arg2><if(exception&&verbose)>: <exception>"
            + "<stackTrace; separator=\"\\n\"><endif>", ErrorSeverity.WARNING),

    /**
     * Compiler Error 36:
     * <p>
     * RESOLVE cannot generate <em>language</em> code as of version
     * <em>version</em>.</p>
     */
    CANNOT_CREATE_TARGET_GENERATOR(36, "RESOLVE cannot generate <arg> code as "
            + "of version " + RESOLVECompiler.VERSION,
            ErrorSeverity.ERROR_ONE_OFF),

    /**
     * Compiler Error 37:
     * <p>
     * no main operation defined in: <em>moduleName</em>; cannot create
     * executable jar without a main.</p>
     */
    NO_MAIN_SPECIFIED(37, "no main() operation defined in: <arg>; "
            + "cannot create executable jar without a main "
            + "(note: main is expected to have zero parameters)",
            ErrorSeverity.ERROR_ONE_OFF),

    /**
     * Compiler Error 38: generated Java error: <em>message</em>.
     */
    GENERATED_JAVA_ERROR(38, "generated java error: <arg>",
            ErrorSeverity.ERROR),

    UNLABELED_RECURSIVE_FUNC(39, "recursive call '<arg>' detected in an " +
            "unmarked recursive procedure: '<arg2>'; should be: " +
            "Oper <arg2>(..); Recursive Procedure ... end <arg2>;",
                         ErrorSeverity.WARNING),

    MISMATCHED_BLOCK_END_NAMES(40, "mismatched block end names: " +
            "'<arg>' != '<arg2>'",
                            ErrorSeverity.WARNING),

    LABELED_NON_RECURSIVE_FUNC(41, "procedure <arg> marked 'Recursive', " +
            "but contains no recursive calls",
            ErrorSeverity.WARNING),

    INCOMPATIBLE_OP_TYPES(42, "incompatible types on <arg> found: " +
            "<arg2>, <arg3>; these need to be the same types",
            ErrorSeverity.ERROR),

    MISSING_RETURN_STMT(43, "operation/procedure: <arg> is missing a return " +
            "assignment stmt (e.g.: the concluding statement should be " +
            "<arg> := [SOME RETURN VALUE];",
            ErrorSeverity.ERROR),

    ILLEGAL_INCOMING_REF_IN_REQUIRES(44, "found illegal '@'-valued " +
            "variable ref(s): [<arg; separator={, }>] in requires " +
            "clause: <arg2>; '@-variables' are not permitted in requires clauses",
            ErrorSeverity.ERROR),

    MALFORMED_RECURSIVE_OP_CALL(56, "inappropriate arguments supplied to " +
            "recursive call: <arg>. i.e.: arguments fail to match in either " +
            "number or type (or both) for recursive operation: <arg2>",
            ErrorSeverity.ERROR),

    SYMBOL_NAME_MATCHES_MODULE_NAME(60, "symbol s=<arg>, " +
            "(which is <arg2>) shares the same name as the enclosing module; " +
            "it needs a unique name", ErrorSeverity.ERROR),

    ILLEGAL_MODE_FOR_FUNCTIONAL_OP(61, "operation <arg> declares a return, " +
            "but parameter(s) [<arg2; separator = ', '>] use mode <arg3>; " +
            "operations with a return can only use these modes: " +
            "[preserves, restores, evaluates]",
            ErrorSeverity.ERROR),

    ILLEGAL_PRIMARY_OPERATION_CALL(62, "procedure '<arg>' references another " +
            "primary operation with: <arg2>; primary operations shouldn't " +
            "reference each other", ErrorSeverity.ERROR);

    public final int code;
    public final String message;

    public final ErrorSeverity severity;

    ErrorKind(int code, String message, ErrorSeverity severity) {
        this.code = code;
        this.message = message;
        this.severity = severity;
    }
}