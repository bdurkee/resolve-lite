package org.resolvelite.semantics;

import org.resolvelite.compiler.ErrorKind;

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