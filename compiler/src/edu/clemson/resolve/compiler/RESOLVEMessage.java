package edu.clemson.resolve.compiler;

import org.antlr.v4.runtime.Token;
import org.stringtemplate.v4.ST;

import java.util.Arrays;

public class RESOLVEMessage {

    private static final Object[] EMPTY_ARGS = new Object[0];

    private final ErrorKind errorType;
    private final Object[] args;
    private final Throwable e;

    // used for location template
    public String fileName;
    public int line = -1;
    public int charPosition = -1;
    public Token offendingToken;

    public RESOLVEMessage(ErrorKind errorKind) {
        this(errorKind, (Throwable) null, null);
    }

    public RESOLVEMessage(ErrorKind errorType, Token offendingToken,
                          Object... args) {
        this(errorType, null, offendingToken, args);
    }

    public RESOLVEMessage(ErrorKind errorType, Throwable e,
                          Token offendingToken, Object... args) {
        this.errorType = errorType;
        this.e = e;
        this.args = args;
        this.offendingToken = offendingToken;
    }

    public ErrorKind getErrorType() {
        return errorType;
    }

    public Object[] getArgs() {
        if ( args == null ) {
            return EMPTY_ARGS;
        }
        return args;
    }

    public ST getMessageTemplate(boolean verbose) {
        ST messageST = new ST(getErrorType().message);
        messageST.impl.name = errorType.name();

        messageST.add("verbose", verbose);
        Object[] args = getArgs();
        for (int i = 0; i < args.length; i++) {
            String attr = "arg";
            if ( i > 0 ) {
                attr += i + 1;
            }
            messageST.add(attr, args[i]);
        }
        if ( args.length < 2 ) {
            messageST.add("arg2", null);
        }
        Throwable cause = getCause();
        if ( cause != null ) {
            messageST.add("exception", cause);
            messageST.add("stackTrace", cause.getStackTrace());
        }
        else {
            messageST.add("exception", null); // avoid ST error msg
            messageST.add("stackTrace", null);
        }
        return messageST;
    }

    public Throwable getCause() {
        return e;
    }

    @Override public String toString() {
        return "Message{"
                + "errorType=" + getErrorType()
                + ", args=" + Arrays.asList(getArgs())
                + ", e=" + getCause()
                + ", fileName='" + fileName + '\''
                + ", line=" + line
                + ", charPosition=" + charPosition
                + '}';
    }
}