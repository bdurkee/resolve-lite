package edu.clemson.resolve.semantics;

import edu.clemson.resolve.compiler.ErrorKind;

@SuppressWarnings("serial")
public class SymbolTableException extends Exception {
    private final ErrorKind errorKind;

    public SymbolTableException(ErrorKind kind) {
        super();
        this.errorKind = kind;
    }

    public ErrorKind getErrorKind() {
        return errorKind;
    }
}