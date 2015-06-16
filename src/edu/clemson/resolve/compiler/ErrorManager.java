package edu.clemson.resolve.compiler;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.tool.ErrorSeverity;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

import java.io.File;
import java.util.EnumSet;
import java.util.Set;

public class ErrorManager extends BaseErrorListener {
    public static final String FORMATS_DIR =
            "edu/resolve/templates/messages/";

    private final STGroup format = new STGroupFile(FORMATS_DIR + "resolve"
            + STGroup.GROUP_FILE_EXTENSION);

    private final ResolveCompiler compiler;
    private int errorCount, warningCount;

    public Set<ErrorKind> errorTypes = EnumSet.noneOf(ErrorKind.class);

    public ErrorManager(ResolveCompiler c) {
        this.compiler = c;
    }

    public int getWarningCount() {
        return warningCount;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public void resetCounts() {
        warningCount = 0;
        errorCount = 0;
    }

    public ST getMessageTemplate(ResolveMessage msg) {
        ST messageST = msg.getMessageTemplate(compiler.longMessages);
        ST locationST = getLocationFormat();
        ST reportST = getReportFormat(msg.getErrorType().severity);
        ST messageFormatST = getMessageFormat();

        boolean locationValid = false;
        if ( msg.line != -1 ) {
            locationST.add("line", msg.line);
            locationValid = true;
        }
        if ( msg.charPosition != -1 ) {
            locationST.add("column", msg.charPosition);
            locationValid = true;
        }
        if ( msg.fileName != null ) {
            File f = new File(msg.fileName);
            // Don't show path to file in edu.clemson.cs.r2jt.templates.messages; too long.
            String displayFileName = msg.fileName;
            if ( f.exists() ) {
                displayFileName = f.getName();
            }
            locationST.add("file", displayFileName);
            locationValid = true;
        }
        messageFormatST.add("id", msg.getErrorType().code);
        messageFormatST.add("text", messageST);

        if ( locationValid ) reportST.add("location", locationST);
        reportST.add("message", messageFormatST);

        return reportST;
    }

    public ST getMessageFormat() {
        return format.getInstanceOf("message");
    }

    public ST getLocationFormat() {
        return format.getInstanceOf("location");
    }

    public ST getReportFormat(ErrorSeverity severity) {
        ST st = format.getInstanceOf("report");
        st.add("type", severity.getText());
        return st;
    }

    @Override public void syntaxError(Recognizer<?, ?> recognizer,
                                      Object offendingSymbol, int line, int charPositionInLine,
                                      String msg, RecognitionException e) {
        ResolveMessage m =
                new LanguageSyntaxMessage(ErrorKind.SYNTAX_ERROR,
                        (Token) offendingSymbol, e, msg);
        emit(ErrorKind.SYNTAX_ERROR, m);
    }

    //Todo: A temporary method that halts on semantic errors. Ideally this
    //will eventually become unnecessary and we can recover and continue
    //processing after encountering an error, for instance: a dup symbol exception.
    public void fatalSemanticError(ErrorKind etype, Token offendingSymbol,
                                   Object... args) {
        semanticError(etype, offendingSymbol, args);
        compiler.exit(1);
    }

    public void semanticError(ErrorKind etype, Token offendingSymbol,
                              Object... args) {
        ResolveMessage msg =
                new LanguageSemanticsMessage(etype, offendingSymbol, args);
        emit(etype, msg);
    }

    /**
     * Raise a predefined message with some number of paramters for the
     * StringTemplate but for which there is no location information possible.
     *
     * @param errorType The Message Descriptor
     * @param args The arguments to pass to the StringTemplate
     */
    public void toolError(ErrorKind errorType, Object... args) {
        toolError(errorType, null, args);
    }

    public void toolError(ErrorKind errorType, Throwable e, Object... args) {
        CompilerMessage msg = new CompilerMessage(errorType, e, args);
        emit(errorType, msg);
    }

    public static void fatalInternalError(String error, Throwable e) {
        internalError(error, e);
        throw new RuntimeException(error, e);
    }

    public static void internalError(String error, Throwable e) {
        StackTraceElement location = getLastNonErrorManagerCodeLocation(e);
        internalError("Exception " + e + "@" + location + ": " + error);
    }

    public static void internalError(String error) {
        StackTraceElement location =
                getLastNonErrorManagerCodeLocation(new Exception());
        String msg = location + ": " + error;
        System.err.println("internal error: " + msg);
    }

    /**
     * Returns first non ErrorManager code location for generating messages.
     */
    private static StackTraceElement getLastNonErrorManagerCodeLocation(
            Throwable e) {
        StackTraceElement[] stack = e.getStackTrace();
        int i = 0;
        for (; i < stack.length; i++) {
            StackTraceElement t = stack[i];
            if ( !t.toString().contains("ErrorManager") ) {
                break;
            }
        }
        StackTraceElement location = stack[i];
        return location;
    }

    public boolean formatWantsSingleLineMessage() {
        return format.getInstanceOf("wantsSingleLineMessage").render()
                .equals("true");
    }

    @SuppressWarnings("fallthrough") public void emit(ErrorKind kind,
                                                      ResolveMessage msg) {
        switch (kind.severity) {
            case WARNING_ONE_OFF:
                if ( errorTypes.contains(kind) ) {
                    break;
                }
                // fall thru
            case WARNING:
                warningCount++;
                compiler.warning(msg);
                break;
            case ERROR_ONE_OFF:
                if ( errorTypes.contains(kind) ) {
                    break;
                }
                // fall thru
            case ERROR:
                errorCount++;
                compiler.error(msg);
                break;
        }
        errorTypes.add(kind);
    }
}
